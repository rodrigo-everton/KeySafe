package com.example.keysafe.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.keysafe.ui.auth.AuthMode

@Composable
fun AuthScreen(
    mode: AuthMode,
    isLoading: Boolean,
    errorMessage: String?,
    onCreateMasterPassword: (String) -> Unit,
    onUnlock: (String) -> Unit,
    biometricConfigured: Boolean = false,
    biometricAvailable: Boolean = false,
    biometricBusy: Boolean = false,
    biometricMessage: String? = null,
    onBiometricUnlock: () -> Unit = {}
) {
    // Nunca usar rememberSaveable para senhas: persiste em savedInstanceState.
    var password by remember(mode) { mutableStateOf("") }
    var confirmation by remember(mode) { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val creating = mode == AuthMode.CREATE
    val matches = password == confirmation
    val canSubmit = !isLoading && when (mode) {
        AuthMode.CREATE -> password.length >= 12 && password.isNotBlank() && matches
        AuthMode.LOCKED -> password.isNotEmpty()
        else -> false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("KeySafe", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Gerenciador de senhas offline", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    when (mode) {
                        AuthMode.CREATE -> "Criar senha mestra"
                        AuthMode.LOCKED -> "Desbloquear cofre"
                        AuthMode.CORRUPTED -> "Problema com o cofre"
                        AuthMode.UNLOCKED -> "Cofre desbloqueado"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    when (mode) {
                        AuthMode.CREATE -> "Primeiro acesso: escolha uma senha mestra com pelo menos 12 caracteres. Ela não poderá ser recuperada."
                        AuthMode.LOCKED -> "Digite sua senha mestra para acessar o cofre."
                        AuthMode.CORRUPTED -> "Os metadados de segurança estão incompletos ou corrompidos. O acesso foi bloqueado para evitar perda de dados."
                        AuthMode.UNLOCKED -> "Acesso autorizado."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (mode == AuthMode.CREATE || mode == AuthMode.LOCKED) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Senha mestra") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Ocultar" else "Mostrar")
                            }
                        }
                    )
                    if (creating) {
                        OutlinedTextField(
                            value = confirmation,
                            onValueChange = { confirmation = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirmar senha mestra") },
                            singleLine = true,
                            enabled = !isLoading,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        if (confirmation.isNotEmpty() && !matches) {
                            Text("As senhas não coincidem.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (errorMessage != null) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val enteredPassword = password
                            password = ""
                            confirmation = ""
                            if (creating) onCreateMasterPassword(enteredPassword)
                            else onUnlock(enteredPassword)
                        },
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Text(if (creating) "Criar e desbloquear" else "Desbloquear")
                        }
                    }
                    if (mode == AuthMode.LOCKED && biometricConfigured) {
                        Button(
                            onClick = onBiometricUnlock,
                            enabled = !isLoading && !biometricBusy && biometricAvailable,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (biometricBusy) "Aguardando biometria…" else "Desbloquear com biometria")
                        }
                        if (!biometricAvailable) {
                            Text(
                                "A biometria forte está indisponível neste dispositivo. Use a senha mestra.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (biometricMessage != null) {
                        Text(biometricMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (mode == AuthMode.CORRUPTED) {
                    Text(
                        "Não reinstale ou apague os dados sem ter um backup criptografado válido.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Sem conexão com a internet. A senha mestra nunca é gravada no armazenamento.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
