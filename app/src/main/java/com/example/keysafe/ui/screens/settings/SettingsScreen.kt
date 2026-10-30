package com.example.keysafe.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLock: () -> Unit = {},
    biometricConfigured: Boolean = false,
    biometricAvailable: Boolean = false,
    biometricBusy: Boolean = false,
    biometricMessage: String? = null,
    onEnableBiometric: () -> Unit = {},
    onDisableBiometric: () -> Unit = {}
) {
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

        Button(onClick = onLock, modifier = Modifier.fillMaxWidth()) {
            Text("Bloquear cofre agora")
        }
        SettingInfoCard(
            title = "Senha mestra",
            description = "Ativada. A senha mestra protege a chave aleatória usada para abrir o cofre."
        )
        SettingInfoCard(
            title = "Autenticação biométrica",
            description = if (biometricConfigured)
                "Ativada: outra cópia cifrada da chave do cofre é protegida pelo Android Keystore. A senha mestra permanece válida."
            else
                "Desativada: habilite após registrar uma biometria forte nas configurações do Android."
        )
        if (biometricConfigured) {
            Button(
                onClick = onDisableBiometric,
                enabled = !biometricBusy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Desativar biometria") }
        } else {
            Button(
                onClick = onEnableBiometric,
                enabled = biometricAvailable && !biometricBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (biometricBusy) "Aguardando confirmação…" else "Ativar desbloqueio biométrico")
            }
            if (!biometricAvailable) {
                Text(
                    "Biometria forte não disponível ou não cadastrada neste dispositivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (biometricMessage != null) {
            Text(biometricMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingInfoCard(
            title = "Bloqueio automático",
            description = "O cofre é bloqueado automaticamente quando o aplicativo vai para segundo plano. Tempo de inatividade dentro do app ainda não implementado."
        )
        SettingInfoCard(
            title = "Armazenamento offline",
            description = "Ativado: credenciais persistidas localmente no Room com todos os campos protegidos por AES-256-GCM. Não há sincronização com servidores."
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
