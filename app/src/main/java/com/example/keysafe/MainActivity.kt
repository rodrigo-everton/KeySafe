package com.example.keysafe

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.keysafe.navigation.AppNavigation
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import com.example.keysafe.ui.credential.CredentialViewModel
import com.example.keysafe.ui.theme.KeySafeTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: MasterPasswordViewModel by viewModels()
    private val credentialViewModel: CredentialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            KeySafeTheme {
                AppNavigation(authViewModel, credentialViewModel)
            }
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            // Limpa primeiro os metadados decifrados, depois a chave AES.
            credentialViewModel.stopSession()
            authViewModel.lock()
        }
        super.onStop()
    }
}
