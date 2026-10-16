package com.example.keysafe.ui.screens.generator

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.security.SecureRandom
import kotlinx.coroutines.launch

private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val DIGITS = "0123456789"
private const val SYMBOLS = "!@#\$%&*()-_=+?"
private val secureRandom = SecureRandom()

/** Garante ao menos um caractere de cada grupo habilitado. */
private fun generateSecurePassword(
    length: Int,
    lower: Boolean,
    upper: Boolean,
    digits: Boolean,
    symbols: Boolean
): String {
    val pools = buildList {
        if (lower) add(LOWER)
        if (upper) add(UPPER)
        if (digits) add(DIGITS)
        if (symbols) add(SYMBOLS)
    }
    if (pools.isEmpty()) return ""
    require(length >= pools.size) { "Comprimento menor que a quantidade de grupos habilitados" }

    val all = pools.joinToString("")
    val chars = pools.map { it[secureRandom.nextInt(it.length)] }.toMutableList()
    repeat(length - chars.size) {
        chars.add(all[secureRandom.nextInt(all.length)])
    }
    for (i in chars.lastIndex downTo 1) {
        val j = secureRandom.nextInt(i + 1)
        val temp = chars[i]
        chars[i] = chars[j]
        chars[j] = temp
    }
    return chars.joinToString("")
}

@Composable
fun PasswordGeneratorScreen(onBack: () -> Unit) {
    var length by remember { mutableIntStateOf(20) }
    var lower by remember { mutableStateOf(true) }
    var upper by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf(generateSecurePassword(20, true, true, true, true)) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val hasSelectedPool = lower || upper || digits || symbols

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("← Voltar") }
            Text("Gerador de senhas", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Gere senhas localmente usando SecureRandom.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (showPassword) password else "•".repeat(password.length),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { showPassword = !showPassword },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (showPassword) "Ocultar senha" else "Mostrar senha")
                    }
                }
            }

            Text("Comprimento: $length caracteres", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = length.toFloat(),
                onValueChange = { length = it.toInt().coerceIn(12, 64) },
                valueRange = 12f..64f,
                steps = 51
            )
            CharacterOption("Letras minúsculas (a-z)", lower) { lower = it }
            CharacterOption("Letras maiúsculas (A-Z)", upper) { upper = it }
            CharacterOption("Números (0-9)", digits) { digits = it }
            CharacterOption("Símbolos (!@#...)", symbols) { symbols = it }

            if (!hasSelectedPool) {
                Text(
                    "Selecione ao menos um tipo de caractere.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = {
                    password = generateSecurePassword(length, lower, upper, digits, symbols)
                    showPassword = false
                },
                enabled = hasSelectedPool,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Gerar nova senha") }

            OutlinedButton(
                onClick = {
                    val clip = ClipData.newPlainText("Senha gerada", password)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        clip.description.extras = PersistableBundle().apply {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                    }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(clip)
                    scope.launch {
                        snackbarHostState.showSnackbar("Senha copiada. Evite mantê-la na área de transferência.")
                    }
                },
                enabled = password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Copiar senha") }
            Text(
                "Se alterar os critérios, toque em “Gerar nova senha” para atualizar o resultado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CharacterOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
