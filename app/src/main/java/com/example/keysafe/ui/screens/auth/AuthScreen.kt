package com.example.keysafe.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Tela inicial. A autenticação real deverá chamar onAuthenticated SOMENTE
 * depois de uma validação bem-sucedida feita pelo Android e pelo cofre.
 *
 * demoMode deve permanecer false nas versões distribuídas.
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    demoMode: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KeySafe",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Seu gerenciador de senhas offline",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Acesso ao cofre",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (demoMode) {
                        "Modo de demonstração: nenhuma autenticação será feita. Não use com senhas reais."
                    } else {
                        "A autenticação biométrica ou por credencial do dispositivo ainda precisa ser integrada."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAuthenticated,
                    enabled = demoMode
                ) {
                    Text(if (demoMode) "Entrar no modo de demonstração" else "Desbloquear cofre")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "O desbloqueio da interface não substitui a proteção da chave de criptografia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
