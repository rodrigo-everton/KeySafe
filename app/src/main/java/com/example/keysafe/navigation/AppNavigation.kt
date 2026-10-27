package com.example.keysafe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.ui.auth.AuthMode
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import com.example.keysafe.ui.credential.CredentialViewModel
import com.example.keysafe.ui.screens.auth.AuthScreen
import com.example.keysafe.ui.screens.credential.AddCredentialScreen
import com.example.keysafe.ui.screens.generator.PasswordGeneratorScreen
import com.example.keysafe.ui.screens.home.HomeScreen
import com.example.keysafe.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    authViewModel: MasterPasswordViewModel,
    credentialViewModel: CredentialViewModel
) {
    val authState = authViewModel.uiState

    // Nenhuma tela do cofre é montada sem autenticação.
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

    // Apenas com sessão desbloqueada: escutar registros criptografados do Room.
    DisposableEffect(authViewModel, credentialViewModel) {
        credentialViewModel.startSession(authViewModel)
        onDispose { credentialViewModel.stopSession() }
    }

    val credentialState by credentialViewModel.uiState.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(
                credentials = credentialState.credentials,
                loading = credentialState.loading,
                errorMessage = credentialState.errorMessage,
                onAddCredential = { navController.navigate(Routes.AddCredential.route) },
                onPasswordGenerator = { navController.navigate(Routes.PasswordGenerator.route) },
                onSettings = { navController.navigate(Routes.Settings.route) }
            )
        }
        composable(Routes.AddCredential.route) {
            AddCredentialScreen(
                onBack = { navController.popBackStack() },
                isSaving = credentialState.saving,
                errorMessage = credentialState.errorMessage,
                onSave = { form ->
                    credentialViewModel.save(
                        auth = authViewModel,
                        draft = CredentialDraft(
                            title = form.title,
                            username = form.username,
                            password = form.password,
                            url = form.url,
                            notes = form.notes,
                            category = form.category,
                            favorite = form.favorite
                        ),
                        onSaved = { navController.popBackStack() }
                    )
                }
            )
        }
        composable(Routes.PasswordGenerator.route) {
            PasswordGeneratorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLock = {
                    credentialViewModel.stopSession()
                    authViewModel.lock()
                }
            )
        }
    }
}
