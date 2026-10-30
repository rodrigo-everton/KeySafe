package com.example.keysafe

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.example.keysafe.navigation.AppNavigation
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import com.example.keysafe.ui.credential.CredentialViewModel
import com.example.keysafe.ui.theme.KeySafeTheme

/** FragmentActivity é obrigatória para a API BiometricPrompt do AndroidX 1.1.0. */
class MainActivity : FragmentActivity() {
    private val authViewModel: MasterPasswordViewModel by viewModels()
    private val credentialViewModel: CredentialViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Reconstruir o BiometricPrompt após rotação registra o callback na Activity atual.
        biometricPrompt = BiometricPrompt(this, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                authViewModel.onBiometricAuthenticated(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                authViewModel.onBiometricPromptError(
                    cancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                )
            }
        })

        setContent {
            KeySafeTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    credentialViewModel = credentialViewModel,
                    onBiometricUnlock = { showBiometricPrompt(enrolling = false) },
                    onEnableBiometric = { showBiometricPrompt(enrolling = true) }
                )
            }
        }
    }

    private fun showBiometricPrompt(enrolling: Boolean) {
        authViewModel.refreshBiometricAvailability()
        if (BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG)
            != BiometricManager.BIOMETRIC_SUCCESS) return

        val cipher = if (enrolling) authViewModel.prepareBiometricEnrollment()
        else authViewModel.prepareBiometricUnlock()
        if (cipher == null) return

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (enrolling) "Ativar biometria no KeySafe" else "Desbloquear KeySafe")
            .setSubtitle("Confirme sua identidade com biometria forte")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText(if (enrolling) "Cancelar" else "Usar senha mestra")
            .build()

        try {
            // Não usar authenticate(promptInfo) sem CryptoObject!
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (_: Exception) {
            authViewModel.onBiometricPromptError(cancelled = false)
        }
    }

    override fun onResume() {
        super.onResume()
        authViewModel.refreshBiometricAvailability()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            // Invalidar callbacks ANTES de cancelar o prompt e limpar a chave da sessão.
            credentialViewModel.stopSession()
            authViewModel.lock()
            if (::biometricPrompt.isInitialized) biometricPrompt.cancelAuthentication()
        }
        super.onStop()
    }
}
