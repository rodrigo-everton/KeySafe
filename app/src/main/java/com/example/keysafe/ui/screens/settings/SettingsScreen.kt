package com.example.keysafe.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onDisableBiometric: () -> Unit = {},
    onFactoryReset: () -> Boolean = { false }
) {
    var showWipeDialog by remember { mutableStateOf(false) }
    var confirmationText by remember { mutableStateOf("") }
    var wipeError by remember { mutableStateOf<String?>(null) }
    var wipeRequested by remember { mutableStateOf(false) }

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

        // Ação separada das configurações comuns e visualmente marcada como destrutiva.
        SettingInfoCard(
            title = "Autodestruição dos dados",
            description = "Apaga os dados locais do KeySafe e restaura o estado de primeiro uso. O aplicativo continuará instalado. Esta operação é permanente e não pode ser desfeita."
        )
        Button(
            onClick = {
                confirmationText = ""
                wipeError = null
                wipeRequested = false
                showWipeDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text("Apagar todos os dados do aplicativo")
        }
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!wipeRequested) {
                    showWipeDialog = false
                    confirmationText = ""
                    wipeError = null
                }
            },
            title = { Text("Apagar permanentemente?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Esta ação apagará todas as credenciais, a senha mestra, a chave do cofre, " +
                            "a configuração biométrica, o banco de dados, as preferências e os arquivos locais."
                    )
                    Text(
                        "O KeySafe continuará instalado. Na próxima abertura, será exibida a tela " +
                            "de configuração inicial, como no primeiro uso. Não será possível recuperar os dados."
                    )
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = {
                            confirmationText = it
                            wipeError = null
                        },
                        enabled = !wipeRequested,
                        singleLine = true,
                        label = { Text("Digite APAGAR para confirmar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (wipeError != null) {
                        Text(
                            wipeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (wipeRequested) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Solicitação aceita. Encerrando o KeySafe…")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = confirmationText == "APAGAR" && !wipeRequested,
                    onClick = {
                        wipeRequested = true
                        val accepted = onFactoryReset()
                        if (!accepted) {
                            wipeRequested = false
                            wipeError = "O Android não conseguiu iniciar a limpeza dos dados. Nenhum dado foi apagado. Tente novamente."
                        }
                    }
                ) {
                    Text("Apagar permanentemente", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !wipeRequested,
                    onClick = {
                        showWipeDialog = false
                        confirmationText = ""
                        wipeError = null
                    }
                ) { Text("Cancelar") }
            }
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
