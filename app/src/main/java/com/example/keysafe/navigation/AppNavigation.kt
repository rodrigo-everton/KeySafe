package com.example.keysafe.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.ui.auth.AuthMode
import com.example.keysafe.ui.auth.MasterPasswordViewModel
import com.example.keysafe.ui.credential.CredentialDetailUiState
import com.example.keysafe.ui.credential.CredentialViewModel
import com.example.keysafe.ui.screens.auth.AuthScreen
import com.example.keysafe.ui.screens.credential.AddCredentialScreen
import com.example.keysafe.ui.screens.credential.CredentialDetailsScreen
import com.example.keysafe.ui.screens.credential.CredentialFormData
import com.example.keysafe.ui.screens.generator.PasswordGeneratorScreen
import com.example.keysafe.ui.screens.home.HomeScreen
import com.example.keysafe.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    authViewModel: MasterPasswordViewModel,
    credentialViewModel: CredentialViewModel,
    onBiometricUnlock: () -> Unit,
    onEnableBiometric: () -> Unit
) {
    val authState = authViewModel.uiState
    if (authState.mode != AuthMode.UNLOCKED) {
        AuthScreen(
            mode = authState.mode,
            isLoading = authState.loading,
            errorMessage = authState.error,
            onCreateMasterPassword = authViewModel::create,
            onUnlock = authViewModel::unlock,
            biometricConfigured = authViewModel.biometricConfigured,
            biometricAvailable = authViewModel.biometricAvailable,
            biometricBusy = authViewModel.biometricBusy,
            biometricMessage = authViewModel.biometricMessage,
            onBiometricUnlock = onBiometricUnlock
        )
        return
    }

    // Mantém o cofre observável somente enquanto está desbloqueado.
    DisposableEffect(authViewModel, credentialViewModel) {
        credentialViewModel.startSession(authViewModel)
        onDispose { credentialViewModel.stopSession() }
    }

    val listState by credentialViewModel.uiState.collectAsState()
    val detailState by credentialViewModel.detailState.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(
                credentials = listState.credentials,
                loading = listState.loading,
                errorMessage = listState.errorMessage,
                onCredentialSelected = { selected ->
                    navController.navigate(Routes.details(selected.id))
                },
                onAddCredential = { navController.navigate(Routes.AddCredential.route) },
                onPasswordGenerator = { navController.navigate(Routes.PasswordGenerator.route) },
                onSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.AddCredential.route) {
            AddCredentialScreen(
                onBack = { navController.popBackStack() },
                isSaving = listState.saving,
                errorMessage = listState.errorMessage,
                onSave = { form ->
                    credentialViewModel.save(authViewModel, form.toDraft()) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(
            route = Routes.CredentialDetails.route,
            arguments = listOf(navArgument("credentialId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("credentialId")
            LaunchedEffect(id) {
                if (id != null) credentialViewModel.loadDetails(authViewModel, id)
            }
            val current = if (detailState.id == id) detailState
                else CredentialDetailUiState(id = id, loading = true)
            CredentialDetailsScreen(
                state = current,
                onBack = {
                    credentialViewModel.clearDetails()
                    navController.popBackStack()
                },
                onEdit = {
                    if (id != null) navController.navigate(Routes.edit(id))
                },
                onDelete = {
                    if (id != null) credentialViewModel.delete(authViewModel, id) {
                        navController.popBackStack(Routes.Home.route, inclusive = false)
                    }
                }
            )
        }

        composable(
            route = Routes.EditCredential.route,
            arguments = listOf(navArgument("credentialId") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("credentialId")
            LaunchedEffect(id) {
                if (id != null) credentialViewModel.loadDetails(authViewModel, id)
            }
            val current = if (detailState.id == id) detailState
                else CredentialDetailUiState(id = id, loading = true)

            when {
                current.loading -> CircularProgressIndicator()
                current.draft == null -> Text(current.errorMessage ?: "Credencial não encontrada.")
                else -> AddCredentialScreen(
                    initialData = current.draft.toFormData(),
                    isEditing = true,
                    isSaving = current.busy,
                    errorMessage = current.errorMessage,
                    onBack = { navController.popBackStack() },
                    onSave = { form ->
                        if (id != null) credentialViewModel.update(authViewModel, id, form.toDraft()) {
                            navController.popBackStack()
                        }
                    }
                )
            }
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
                },
                biometricConfigured = authViewModel.biometricConfigured,
                biometricAvailable = authViewModel.biometricAvailable,
                biometricBusy = authViewModel.biometricBusy,
                biometricMessage = authViewModel.biometricMessage,
                onEnableBiometric = onEnableBiometric,
                onDisableBiometric = authViewModel::disableBiometrics
            )
        }
    }
}

private fun CredentialFormData.toDraft() = CredentialDraft(
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category,
    favorite = favorite
)

private fun CredentialDraft.toFormData() = CredentialFormData(
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category,
    favorite = favorite
)
