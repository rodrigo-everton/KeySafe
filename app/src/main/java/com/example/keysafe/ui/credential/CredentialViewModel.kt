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

/**
 * Mantém SOMENTE resumos decifrados enquanto o cofre está desbloqueado.
 * Não mantém a senha da credencial em listas ou em estados observáveis.
 */
data class CredentialUiState(
    val credentials: List<CredentialSummary> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorMessage: String? = null
)

class CredentialViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CredentialRepository(
        CredentialDatabase.getInstance(application).credentialDao()
    )

    private val _uiState = MutableStateFlow(CredentialUiState())
    val uiState: StateFlow<CredentialUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var saveJob: Job? = null
    private var generation: Long = 0L

    /** Começa a observar o Room apenas dentro de uma sessão autenticada. */
    fun startSession(auth: MasterPasswordViewModel) {
        stopSession()
        if (auth.uiState.mode != AuthMode.UNLOCKED) return
        val session = generation
        _uiState.value = CredentialUiState()
        observeJob = viewModelScope.launch {
            try {
                repository.observeEncrypted().collectLatest { rows ->
                    // Faz o uso da chave na thread principal, dentro do bloco seguro.
                    val summaries = auth.withUnlockedVaultKey { key ->
                        rows.map { repository.decryptSummary(it, key) }
                            .sortedByDescending { it.updatedAt }
                    }
                    if (session == generation && auth.uiState.mode == AuthMode.UNLOCKED) {
                        _uiState.update { it.copy(credentials = summaries, loading = false, errorMessage = null) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (session == generation && auth.uiState.mode == AuthMode.UNLOCKED) {
                    // Não faz fallback para conteúdo parcialmente decifrado.
                    _uiState.update { it.copy(credentials = emptyList(), loading = false,
                        errorMessage = "Falha ao ler uma credencial. O banco pode estar corrompido.") }
                }
            }
        }
    }

    /** Cancela leituras/gravações pendentes e descarta resumos decifrados. */
    fun stopSession() {
        generation++
        observeJob?.cancel()
        saveJob?.cancel()
        observeJob = null
        saveJob = null
        _uiState.value = CredentialUiState(credentials = emptyList(), loading = false)
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
                // CRIPTOGRAFIA ANTES do DAO. A senha nunca é enviada para o SQLite.
                val encrypted = auth.withUnlockedVaultKey { key -> repository.encrypt(draft, key) }
                repository.insertEncrypted(encrypted)
                if (session == generation && auth.uiState.mode == AuthMode.UNLOCKED) {
                    _uiState.update { it.copy(saving = false, errorMessage = null) }
                    onSaved()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (session == generation && auth.uiState.mode == AuthMode.UNLOCKED) {
                    _uiState.update { it.copy(saving = false,
                        errorMessage = "Não foi possível salvar a credencial.") }
                }
            }
        }
    }

    override fun onCleared() {
        stopSession()
        super.onCleared()
    }
}
