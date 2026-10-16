package com.example.keysafe.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.keysafe.data.model.Credential

@Composable
fun HomeScreen(
    onAddCredential: () -> Unit,
    onPasswordGenerator: () -> Unit,
    onSettings: () -> Unit,
    credentials: List<Credential> = emptyList(),
    onCredentialSelected: ((Credential) -> Unit)? = null
) {
    var search by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val filteredCredentials = credentials.filter { credential ->
        (!favoritesOnly || credential.favorite) && (
            credential.title.contains(search, ignoreCase = true) ||
                credential.username.contains(search, ignoreCase = true) ||
                credential.url.contains(search, ignoreCase = true) ||
                credential.category.contains(search, ignoreCase = true)
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Meu cofre", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${credentials.size} credenciais • ${credentials.count { it.favorite }} favoritas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onSettings) { Text("Configurações") }
            }
        }

        item {
            Button(onClick = onAddCredential, modifier = Modifier.fillMaxWidth()) {
                Text("Adicionar credencial")
            }
        }
        item {
            OutlinedButton(onClick = onPasswordGenerator, modifier = Modifier.fillMaxWidth()) {
                Text("Gerador de senhas")
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Pesquisar credenciais") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Somente favoritas", modifier = Modifier.weight(1f))
                Switch(checked = favoritesOnly, onCheckedChange = { favoritesOnly = it })
            }
        }

        if (filteredCredentials.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (credentials.isEmpty()) "Nenhuma credencial cadastrada" else "Nenhum resultado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (credentials.isEmpty()) {
                                "As senhas aparecerão aqui após a integração com o banco de dados criptografado."
                            } else {
                                "Tente outra pesquisa ou desative o filtro de favoritas."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(filteredCredentials, key = { it.id }) { credential ->
                CredentialCard(
                    credential = credential,
                    onOpen = onCredentialSelected?.let { action -> { action(credential) } }
                )
            }
        }
    }
}

@Composable
private fun CredentialCard(
    credential: Credential,
    onOpen: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = credential.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (credential.favorite) Text("★", color = MaterialTheme.colorScheme.primary)
            }
            if (credential.username.isNotBlank()) {
                Text(credential.username, style = MaterialTheme.typography.bodyMedium)
            }
            if (credential.category.isNotBlank()) {
                Text(
                    credential.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onOpen != null) {
                TextButton(onClick = onOpen) { Text("Ver detalhes") }
            }
        }
    }
}
