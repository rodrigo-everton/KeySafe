# KeySafe — ícone para copiar senhas

Esta atualização adiciona um botão de ícone `content_copy` diretamente ao lado da senha nas telas:

- `PasswordGeneratorScreen.kt`: copia a senha gerada a partir do cartão de resultado. O botão de texto largo "Copiar senha" foi removido.
- `CredentialDetailsScreen.kt`: copia a senha da credencial aberta, mesmo quando ela está visualmente oculta. O valor continua oculto na tela até o usuário escolher "Mostrar senha".

## Instalação

Substitua os dois arquivos nos caminhos correspondentes:

```text
app/src/main/java/com/example/keysafe/ui/screens/generator/PasswordGeneratorScreen.kt
app/src/main/java/com/example/keysafe/ui/screens/credential/CredentialDetailsScreen.kt
```

## Dependência necessária

Os arquivos usam `Icons.Outlined.ContentCopy`. Se ainda não tiver a biblioteca de ícones estendidos no projeto, adicione em `app/build.gradle.kts`:

```kotlin
implementation("androidx.compose.material:material-icons-extended")
```

Se o projeto utiliza Compose BOM, não é necessário informar uma versão nessa dependência. Mantenha o BOM e as demais versões já configuradas no projeto.

## Comportamento de segurança

- A cópia usa o `ClipboardManager` do Android.
- No Android 13 (API 33) ou superior, o conteúdo é marcado como sensível com `ClipDescription.EXTRA_IS_SENSITIVE`, reduzindo a exposição em prévias da área de transferência.
- Um Snackbar confirma a cópia.
- O aplicativo não limpa a área de transferência automaticamente, para não apagar conteúdo que o usuário possa ter copiado depois.

## Validação

1. Abra o gerador, gere uma senha e toque no ícone de copiar.
2. Cole em um campo de texto de teste e confirme que o conteúdo corresponde à senha gerada.
3. Abra os detalhes de uma credencial, mantenha a senha oculta e toque no ícone.
4. Cole em um campo de texto de teste e confirme que a senha correta foi copiada.
5. Confirme que o Snackbar aparece nas duas telas.

A atualização foi conferida estruturalmente, mas deve ser compilada e testada no Android Studio dentro do projeto completo.
