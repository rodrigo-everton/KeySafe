package com.example.keysafe.navigation

sealed class Routes(val route: String) {
    data object Auth : Routes("auth")
    data object Home : Routes("home")
    data object AddCredential : Routes("add_credential")
    data object CredentialDetails : Routes("credential/{credentialId}")
    data object EditCredential : Routes("credential/{credentialId}/edit")
    data object PasswordGenerator : Routes("password_generator")
    data object Settings : Routes("settings")

    companion object {
        // UUIDs gerados pelo aplicativo, não conteúdo digitado pelo usuário.
        fun details(id: String) = "credential/$id"
        fun edit(id: String) = "credential/$id/edit"
    }
}
