package com.example.keysafe.ui.credential

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keysafe.data.local.CredentialDatabase
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.data.model.CredentialSummary
import com.example.keysafe.data.repository.CredentialRepository
import com.example.keysafe.ui.auth.AuthMode
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Somente metadados — nunca colocar a senha na lista. */
data class CredentialUiState(
    val credentials: List<CredentialSummary> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorMessage: String? = null
)

/** Dados de detalhes devem existir apenas enquanto uma sessão está desbloqueada. */
data class CredentialDetailUiState(
    val id: String? = null,
    val loading: Boolean = false,
    val draft: CredentialDraft? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val busy: Boolean = false,
    val errorMessage: String? = null
)

class CredentialViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CredentialRepository(
        CredentialDatabase.getInstance(application).credentialDao()
    )

    private val _uiState = MutableStateFlow(CredentialUiState())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(CredentialDetailUiState())
    val detailState: StateFlow<CredentialDetailUiState> = _detailState.asStateFlow()

    private var observeJob: Job? = null
    private var saveJob: Job? = null
    private var detailJob: Job? = null
    private var mutationJob: Job? = null
    private var generation = 0L
    private var detailRequest = 0L

    fun startSession(auth: MasterPasswordViewModel) {
        stopSession()
        if (auth.uiState.mode != AuthMode.UNLOCKED) return
        val session = generation
        _uiState.value = CredentialUiState()

        observeJob = viewModelScope.launch {
            try {
                repository.observeEncrypted().collectLatest { encryptedRows ->
                    val summaries = auth.withUnlockedVaultKey { key ->
                        encryptedRows.map { repository.decryptSummary(it, key) }
                            .sortedByDescending { it.updatedAt }
                    }
                    if (sessionAlive(session, auth)) {
                        _uiState.update {
                            it.copy(credentials = summaries, loading = false, errorMessage = null)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (sessionAlive(session, auth)) {
                    _uiState.update {
                        it.copy(
                            credentials = emptyList(), loading = false,
                            errorMessage = "Falha ao ler o cofre. Verifique a integridade dos dados."
                        )
                    }
                }
            }
        }
    }

    fun stopSession() {
        generation++
        detailRequest++
        observeJob?.cancel()
        saveJob?.cancel()
        detailJob?.cancel()
        mutationJob?.cancel()
        observeJob = null
        saveJob = null
        detailJob = null
        mutationJob = null
        _uiState.value = CredentialUiState(loading = false)
        // Descarta o conteúdo decifrado dos detalhes (inclusive a senha) ao bloquear.
        _detailState.value = CredentialDetailUiState()
    }

    fun save(
        auth: MasterPasswordViewModel,
        draft: CredentialDraft,
        onSaved: () -> Unit
    ) {
        if (auth.uiState.mode != AuthMode.UNLOCKED || _uiState.value.saving) return
        val session = generation
        _uiState.update { it.copy(saving = true, errorMessage = null) }
        saveJob = viewModelScope.launch {
            try {
                val encrypted = auth.withUnlockedVaultKey { key -> repository.encrypt(draft, key) }
                repository.insertEncrypted(encrypted)
                if (sessionAlive(session, auth)) {
                    _uiState.update { it.copy(saving = false, errorMessage = null) }
                    onSaved()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (sessionAlive(session, auth)) {
                    _uiState.update {
                        it.copy(saving = false, errorMessage = "Não foi possível salvar a credencial.")
                    }
                }
            }
        }
    }

    /** Busca somente o UUID solicitado; o Room retorna os bytes cifrados. */
    fun loadDetails(auth: MasterPasswordViewModel, id: String) {
        if (auth.uiState.mode != AuthMode.UNLOCKED) return
        if (_detailState.value.id == id && _detailState.value.draft != null) return
        if (_detailState.value.busy) return

        detailJob?.cancel()
        val request = ++detailRequest
        val session = generation
        _detailState.value = CredentialDetailUiState(id = id, loading = true)

        detailJob = viewModelScope.launch {
            try {
                val encrypted = repository.findEncrypted(id)
                    ?: error("Credencial não encontrada.")
                val details = auth.withUnlockedVaultKey { key ->
                    repository.decryptDetails(encrypted, key)
                }
                if (sessionAlive(session, auth) && request == detailRequest) {
                    _detailState.value = CredentialDetailUiState(
                        id = details.id,
                        draft = details.draft,
                        createdAt = details.createdAt,
                        updatedAt = details.updatedAt
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (sessionAlive(session, auth) && request == detailRequest) {
                    _detailState.value = CredentialDetailUiState(
                        id = id,
                        errorMessage = "Não foi possível abrir a credencial."
                    )
                }
            }
        }
    }

    fun clearDetails() {
        detailRequest++
        detailJob?.cancel()
        // Uma edição/exclusão em andamento não deve ser ignorada pela navegação.
        // O bloqueio real da sessão continua cancelando mutationJob.
        _detailState.value = CredentialDetailUiState()
    }

    fun update(
        auth: MasterPasswordViewModel,
        id: String,
        draft: CredentialDraft,
        onUpdated: () -> Unit
    ) {
        if (!canMutate(auth, id)) return
        val session = generation
        _detailState.update { it.copy(busy = true, errorMessage = null) }

        mutationJob = viewModelScope.launch {
            try {
                val current = repository.findEncrypted(id)
                    ?: error("Credencial não encontrada.")
                val updated = auth.withUnlockedVaultKey { key ->
                    repository.encryptUpdate(current, draft, key)
                }
                check(repository.updateEncrypted(updated) == 1) {
                    "A credencial não existe mais."
                }
                if (sessionAlive(session, auth)) {
                    val details = auth.withUnlockedVaultKey { key ->
                        repository.decryptDetails(updated, key)
                    }
                    _detailState.value = CredentialDetailUiState(
                        id = id,
                        draft = details.draft,
                        createdAt = details.createdAt,
                        updatedAt = details.updatedAt
                    )
                    onUpdated()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (sessionAlive(session, auth)) {
                    _detailState.update {
                        it.copy(busy = false, errorMessage = "Não foi possível atualizar a credencial.")
                    }
                }
            }
        }
    }

    fun delete(
        auth: MasterPasswordViewModel,
        id: String,
        onDeleted: () -> Unit
    ) {
        if (!canMutate(auth, id)) return
        val session = generation
        _detailState.update { it.copy(busy = true, errorMessage = null) }

        mutationJob = viewModelScope.launch {
            try {
                val encrypted = repository.findEncrypted(id)
                    ?: error("Credencial não encontrada.")
                // Não excluir silenciosamente registros que falham na autenticação AES-GCM.
                auth.withUnlockedVaultKey { key -> repository.decryptSummary(encrypted, key) }
                check(repository.deleteById(id) == 1) {
                    "A credencial não existe mais."
                }
                if (sessionAlive(session, auth)) {
                    clearDetails()
                    onDeleted()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (sessionAlive(session, auth)) {
                    _detailState.update {
                        it.copy(busy = false, errorMessage = "Não foi possível excluir a credencial.")
                    }
                }
            }
        }
    }

    private fun canMutate(auth: MasterPasswordViewModel, id: String): Boolean =
        auth.uiState.mode == AuthMode.UNLOCKED &&
            _detailState.value.id == id &&
            _detailState.value.draft != null &&
            !_detailState.value.busy

    private fun sessionAlive(session: Long, auth: MasterPasswordViewModel): Boolean =
        session == generation && auth.uiState.mode == AuthMode.UNLOCKED

    override fun onCleared() {
        stopSession()
        super.onCleared()
    }
}
