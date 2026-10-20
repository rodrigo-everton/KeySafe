package com.example.keysafe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keysafe.ui.auth.AuthMode
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import com.example.keysafe.ui.screens.auth.AuthScreen
import com.example.keysafe.ui.screens.credential.AddCredentialScreen
import com.example.keysafe.ui.screens.generator.PasswordGeneratorScreen
import com.example.keysafe.ui.screens.home.HomeScreen
import com.example.keysafe.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(authViewModel: MasterPasswordViewModel) {
    val authState = authViewModel.uiState

    // Gate de segurança: nenhuma rota do cofre é montada enquanto bloqueado.
    // O NavController nasce apenas na sessão desbloqueada, sem pilha antiga.
    if (authState.mode != AuthMode.UNLOCKED) {
        AuthScreen(
            mode = authState.mode,
            isLoading = authState.loading,
            errorMessage = authState.error,
            onCreateMasterPassword = authViewModel::create,
            onUnlock = authViewModel::unlock
        )
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(
                onAddCredential = { navController.navigate(Routes.AddCredential.route) },
                onPasswordGenerator = { navController.navigate(Routes.PasswordGenerator.route) },
                onSettings = { navController.navigate(Routes.Settings.route) }
            )
        }
        composable(Routes.AddCredential.route) {
            AddCredentialScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PasswordGenerator.route) {
            PasswordGeneratorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLock = authViewModel::lock
            )
        }
    }
}
