package com.example.keysafe.ui.auth

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keysafe.security.IncorrectMasterPasswordException
import com.example.keysafe.security.MasterPasswordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

enum class AuthMode { CREATE, LOCKED, UNLOCKED, CORRUPTED }

data class AuthUiState(
    val mode: AuthMode,
    val loading: Boolean = false,
    val error: String? = null
)

class MasterPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MasterPasswordStore(application)

    var uiState by mutableStateOf(AuthUiState(initialMode()))
        private set

    private var vaultKey: ByteArray? = null
    private var operationGeneration = 0L

    fun create(masterPassword: String) {
        if (uiState.loading || uiState.mode != AuthMode.CREATE) return
        if (masterPassword.length < 12 || masterPassword.isBlank()) {
            uiState = uiState.copy(error = "Use uma senha mestra de pelo menos 12 caracteres.")
            return
        }
        authenticate(masterPassword, creating = true)
    }

    fun unlock(masterPassword: String) {
        if (uiState.loading || uiState.mode != AuthMode.LOCKED) return
        if (masterPassword.isEmpty()) {
            uiState = uiState.copy(error = "Informe a senha mestra.")
            return
        }
        authenticate(masterPassword, creating = false)
    }

    private fun authenticate(password: String, creating: Boolean) {
        val generation = ++operationGeneration
        uiState = uiState.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                val unlockedKey = withContext(Dispatchers.Default) {
                    val chars = password.toCharArray()
                    try {
                        if (creating) store.create(chars) else store.unlock(chars)
                    } finally {
                        chars.fill('\u0000')
                    }
                }
                if (generation != operationGeneration) {
                    unlockedKey.fill(0)
                    return@launch
                }
                vaultKey?.fill(0)
                vaultKey = unlockedKey
                uiState = AuthUiState(AuthMode.UNLOCKED)
            } catch (_: IncorrectMasterPasswordException) {
                if (generation == operationGeneration) {
                    uiState = AuthUiState(AuthMode.LOCKED, error = "Senha mestra incorreta.")
                }
            } catch (_: Exception) {
                if (generation == operationGeneration) {
                    uiState = AuthUiState(
                        mode = initialMode(),
                        error = "Não foi possível acessar o cofre. Verifique o armazenamento."
                    )
                }
            }
        }
    }

    /** Exige autenticação novamente. Chamar quando o app vai para segundo plano. */
    fun lock() {
        operationGeneration++
        vaultKey?.fill(0)
        vaultKey = null
        uiState = AuthUiState(initialMode())
    }

    /** Fornece a chave AES do cofre SOMENTE enquanto ele está desbloqueado. */
    fun <T> withUnlockedVaultKey(action: (SecretKey) -> T): T {
        val activeKey = vaultKey ?: error("O cofre está bloqueado.")
        return action(SecretKeySpec(activeKey, "AES"))
    }

    private fun initialMode(): AuthMode = when (store.status()) {
        MasterPasswordStore.Status.NEEDS_SETUP -> AuthMode.CREATE
        MasterPasswordStore.Status.LOCKED -> AuthMode.LOCKED
        MasterPasswordStore.Status.CORRUPTED -> AuthMode.CORRUPTED
    }

    override fun onCleared() {
        lock()
        super.onCleared()
    }
}
