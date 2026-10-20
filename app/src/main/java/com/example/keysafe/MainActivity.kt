package com.example.keysafe

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.keysafe.navigation.AppNavigation
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import com.example.keysafe.ui.theme.KeySafeTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: MasterPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Evita capturas da interface e miniaturas legíveis na tela de apps recentes.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            KeySafeTheme {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }

    override fun onStop() {
        // Uma rotação de tela não deve forçar um novo login.
        if (!isChangingConfigurations) authViewModel.lock()
        super.onStop()
    }
}
