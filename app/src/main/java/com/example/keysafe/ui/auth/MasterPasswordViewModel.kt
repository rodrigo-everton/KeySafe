package com.example.keysafe.ui.auth

import android.app.Application
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keysafe.security.BiometricVaultStore
import com.example.keysafe.security.IncorrectMasterPasswordException
import com.example.keysafe.security.MasterPasswordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

enum class AuthMode { CREATE, LOCKED, UNLOCKED, CORRUPTED }

data class AuthUiState(
    val mode: AuthMode,
    val loading: Boolean = false,
    val error: String? = null
)

class MasterPasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val masterStore = MasterPasswordStore(application)
    private val biometricStore = BiometricVaultStore(application)

    var uiState by mutableStateOf(AuthUiState(initialMode()))
        private set

    var biometricConfigured by mutableStateOf(biometricStore.isConfigured())
        private set
    var biometricAvailable by mutableStateOf(false)
        private set
    var biometricBusy by mutableStateOf(false)
        private set
    var biometricMessage by mutableStateOf<String?>(null)
        private set

    private var vaultKey: ByteArray? = null
    private var operationGeneration = 0L

    private enum class BiometricOperation { ENROLL, UNLOCK }
    private var pendingBiometric: BiometricOperation? = null
    private var pendingCipher: Cipher? = null

    init { refreshBiometricAvailability() }

    /** Consultar novamente após voltar das configurações de biometria do Android. */
    fun refreshBiometricAvailability() {
        biometricAvailable = BiometricManager.from(getApplication<Application>())
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        biometricConfigured = biometricStore.isConfigured()
    }

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
        clearPendingBiometric()
        val generation = ++operationGeneration
        biometricMessage = null
        uiState = uiState.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                val unlockedKey = withContext(Dispatchers.Default) {
                    val chars = password.toCharArray()
                    try {
                        if (creating) masterStore.create(chars) else masterStore.unlock(chars)
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
                refreshBiometricAvailability()
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

    /** Chamar apenas com o aplicativo em primeiro plano e BIOMETRIC_STRONG disponível. */
    fun prepareBiometricUnlock(): Cipher? {
        if (uiState.mode != AuthMode.LOCKED || uiState.loading || biometricBusy) return null
        refreshBiometricAvailability()
        if (!biometricConfigured || !biometricAvailable) {
            biometricMessage = "Biometria indisponível. Use a senha mestra."
            return null
        }
        return try {
            val cipher = biometricStore.beginUnlock()
            pendingBiometric = BiometricOperation.UNLOCK
            pendingCipher = cipher
            biometricBusy = true
            biometricMessage = null
            cipher
        } catch (_: Exception) {
            clearPendingBiometric()
            biometricMessage = "Não foi possível usar a chave biométrica. Entre com a senha mestra e reconfigure a biometria."
            null
        }
    }

    /** Só permitir ativação depois de desbloquear via senha mestra ou biometria já autorizada. */
    fun prepareBiometricEnrollment(): Cipher? {
        if (uiState.mode != AuthMode.UNLOCKED || vaultKey == null || biometricBusy) return null
        refreshBiometricAvailability()
        if (biometricConfigured) {
            biometricMessage = "Desative a biometria atual antes de ativá-la novamente."
            return null
        }
        if (!biometricAvailable) {
            biometricMessage = "Configure uma biometria forte no Android para ativar este recurso."
            return null
        }
        return try {
            val cipher = biometricStore.beginEnrollment()
            pendingBiometric = BiometricOperation.ENROLL
            pendingCipher = cipher
            biometricBusy = true
            biometricMessage = null
            cipher
        } catch (_: Exception) {
            clearPendingBiometric()
            biometricMessage = "Não foi possível preparar o cadastro biométrico."
            null
        }
    }

    /** Chamar SOMENTE com o Cipher devolvido por BiometricPrompt.AuthenticationResult. */
    fun onBiometricAuthenticated(authenticatedCipher: Cipher?) {
        val operation = pendingBiometric
        val originalCipher = pendingCipher
        clearPendingBiometric()

        // Evita que callback atrasado, sem CryptoObject ou de outra operação abra o cofre.
        if (operation == null || originalCipher == null) return
        if (authenticatedCipher == null || authenticatedCipher !== originalCipher) {
            biometricMessage = "Não foi possível validar a operação biométrica. Use a senha mestra."
            return
        }

        try {
            when (operation) {
                BiometricOperation.UNLOCK -> {
                    if (uiState.mode != AuthMode.LOCKED || uiState.loading) return
                    val unlockedKey = biometricStore.finishUnlock(authenticatedCipher)
                    vaultKey?.fill(0)
                    vaultKey = unlockedKey
                    biometricMessage = null
                    uiState = AuthUiState(AuthMode.UNLOCKED)
                }
                BiometricOperation.ENROLL -> {
                    if (uiState.mode != AuthMode.UNLOCKED) return
                    val copiedKey = vaultKey?.copyOf() ?: return
                    try {
                        biometricStore.finishEnrollment(authenticatedCipher, copiedKey)
                    } finally {
                        copiedKey.fill(0)
                    }
                    biometricConfigured = biometricStore.isConfigured()
                    biometricMessage = "Biometria ativada. A senha mestra continua disponível."
                }
            }
        } catch (_: Exception) {
            biometricMessage = if (operation == BiometricOperation.UNLOCK) {
                "Não foi possível abrir o cofre com biometria. Use a senha mestra e reconfigure nas Configurações."
            } else {
                "Falha ao ativar biometria. Tente novamente."
            }
        }
    }

    fun onBiometricPromptError(cancelled: Boolean) {
        if (pendingBiometric == null) return
        clearPendingBiometric()
        biometricMessage = if (cancelled) null
        else "Autenticação biométrica indisponível ou cancelada pelo sistema. Use a senha mestra."
    }

    /** Revogar somente a alternativa biométrica. O conteúdo do cofre não é removido. */
    fun disableBiometrics() {
        if (uiState.mode != AuthMode.UNLOCKED || biometricBusy) return
        try {
            biometricStore.disable()
            biometricConfigured = false
            biometricMessage = "Biometria desativada. A senha mestra continua funcionando."
        } catch (_: Exception) {
            biometricConfigured = biometricStore.isConfigured()
            biometricMessage = "Não foi possível desativar completamente a biometria."
        }
    }

    private fun clearPendingBiometric() {
        pendingBiometric = null
        pendingCipher = null
        biometricBusy = false
    }

    /** Bloqueio de segurança: descarta a chave em memória e ignora callbacks pendentes. */
    fun lock() {
        operationGeneration++
        clearPendingBiometric()
        biometricMessage = null
        vaultKey?.fill(0)
        vaultKey = null
        uiState = AuthUiState(initialMode())
    }

    fun <T> withUnlockedVaultKey(action: (SecretKey) -> T): T {
        check(uiState.mode == AuthMode.UNLOCKED) { "O cofre está bloqueado." }
        val activeKey = vaultKey ?: error("O cofre está bloqueado.")
        return action(SecretKeySpec(activeKey, "AES"))
    }

    private fun initialMode(): AuthMode = when (masterStore.status()) {
        MasterPasswordStore.Status.NEEDS_SETUP -> AuthMode.CREATE
        MasterPasswordStore.Status.LOCKED -> AuthMode.LOCKED
        MasterPasswordStore.Status.CORRUPTED -> AuthMode.CORRUPTED
    }

    override fun onCleared() {
        lock()
        super.onCleared()
    }
}
