package com.example.keysafe.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Voltar") }
        Text("Configurações", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Segurança e preferências do cofre",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingInfoCard(
            title = "Autenticação biométrica",
            description = "Pendente: integrar o BiometricPrompt e o desbloqueio da chave criptográfica."
        )
        SettingInfoCard(
            title = "Bloqueio automático",
            description = "Pendente: exigir nova autenticação após inatividade ou ao retornar do segundo plano."
        )
        SettingInfoCard(
            title = "Armazenamento offline",
            description = "Pendente: conectar Room, Repository e criptografia dos campos sensíveis. Estas telas não enviam dados para servidores."
        )
        SettingInfoCard(
            title = "Backup e recuperação",
            description = "Pendente: implementar exportação criptografada, importação e gestão segura de chaves."
        )
        SettingInfoCard(
            title = "Sobre o aplicativo",
            description = "KeySafe — Projeto Integrador 3. Gerenciador de senhas offline desenvolvido em Kotlin e Jetpack Compose."
        )
    }
}

@Composable
private fun SettingInfoCard(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
