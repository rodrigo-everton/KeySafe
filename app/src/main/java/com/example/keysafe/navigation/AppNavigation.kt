package com.example.keysafe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.keysafe.ui.screens.auth.AuthScreen
import com.example.keysafe.ui.screens.home.HomeScreen
import com.example.keysafe.ui.screens.credential.AddCredentialScreen
import com.example.keysafe.ui.screens.generator.PasswordGeneratorScreen
import com.example.keysafe.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Auth.route
    ) {

        // Tela de autenticação
        composable(Routes.Auth.route) {
            AuthScreen(
                demoMode = true, // Somente durante o desenvolvimento
                onAuthenticated = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Auth.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Tela principal
        composable(Routes.Home.route) {
            HomeScreen(
                onAddCredential = {
                    navController.navigate(Routes.AddCredential.route)
                },
                onPasswordGenerator = {
                    navController.navigate(Routes.PasswordGenerator.route)
                },
                onSettings = {
                    navController.navigate(Routes.Settings.route)
                }
            )
        }

        // Adicionar credencial
        composable(Routes.AddCredential.route) {
            AddCredentialScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Gerador de senha
        composable(Routes.PasswordGenerator.route) {
            PasswordGeneratorScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Configurações
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}