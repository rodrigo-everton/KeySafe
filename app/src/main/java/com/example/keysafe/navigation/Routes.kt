package com.example.keysafe.navigation

sealed class Routes(val route: String) {
    data object Auth: Routes("auth")
    data object Home: Routes("home")
    data object AddCredential: Routes("add_credential")
    data object CredentialDetails : Routes("credential/{credentialId}")
    data object PasswordGenerator: Routes("password_generator")
    data object Settings: Routes("settings")
}