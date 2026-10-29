package com.example.keysafe.ui.screens.credential

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.keysafe.ui.credential.CredentialDetailUiState
import java.text.DateFormat
import java.util.Date

@Composable
fun CredentialDetailsScreen(
    state: CredentialDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showPassword by remember(state.id) { mutableStateOf(false) }
    var confirmDelete by remember(state.id) { mutableStateOf(false) }

    val deletingCredential = state.draft
    if (confirmDelete && deletingCredential != null) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) confirmDelete = false },
            title = { Text("Excluir credencial?") },
            text = {
                Text(
                    "A credencial '${deletingCredential.title}' será excluída permanentemente. " +
                        "Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = {
                        confirmDelete = false
                        showPassword = false
                        onDelete()
                    }
                ) { Text("Excluir definitivamente", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !state.busy, onClick = { confirmDelete = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TextButton(onClick = onBack, enabled = !state.busy) { Text("← Voltar ao cofre") }
            Text("Detalhes da credencial", style = MaterialTheme.typography.headlineMedium)

            if (state.loading) {
                CircularProgressIndicator()
                Text("Desbloqueando os dados da credencial...")
            } else if (state.draft == null) {
                Text(
                    state.errorMessage ?: "Credencial não encontrada.",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val value = state.draft
                Text(value.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                DetailLine("Usuário / e-mail", value.username)
                HorizontalDivider()
                Text("Senha", style = MaterialTheme.typography.labelLarge)
                Text(
                    if (showPassword) value.password else "••••••••",
                    style = MaterialTheme.typography.bodyLarge
                )
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Ocultar senha" else "Mostrar senha")
                }
                HorizontalDivider()
                DetailLine("Site", value.url)
                DetailLine("Categoria", value.category)
                DetailLine("Favorita", if (value.favorite) "Sim" else "Não")
                DetailLine("Anotações", value.notes)
                state.createdAt?.let { DetailLine("Criada em", formatDate(it)) }
                state.updatedAt?.let { DetailLine("Atualizada em", formatDate(it)) }

                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showPassword = false
                        onEdit()
                    }
                ) { Text("Editar credencial") }

                OutlinedButton(
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmDelete = true }
                ) { Text("Excluir credencial", color = MaterialTheme.colorScheme.error) }

                if (state.busy) Text("Processando operação...")
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
