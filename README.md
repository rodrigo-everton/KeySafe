# KeySafe — Autodestruição / redefinição completa dos dados

Esta entrega adiciona às Configurações um botão que solicita ao Android a limpeza de todos os dados de usuário do próprio aplicativo, retornando o KeySafe ao estado de primeiro uso.

## Arquivos incluídos

```text
app/src/main/java/com/example/keysafe/
├── MainActivity.kt                                  [SUBSTITUI]
├── navigation/AppNavigation.kt                     [SUBSTITUI]
└── ui/screens/settings/SettingsScreen.kt           [SUBSTITUI]
```

Não há alteração no esquema Room, nos modelos, na criptografia, na senha mestra ou no `BiometricVaultStore`. Nenhuma dependência Gradle ou permissão nova é necessária.

## Como funciona

1. O usuário precisa estar com o cofre desbloqueado para acessar Configurações.
2. Pressiona **Apagar todos os dados do aplicativo**.
3. Um diálogo explica o que será apagado e exige digitar exatamente `APAGAR`.
4. `MainActivity` chama `ActivityManager.clearApplicationUserData()`.
5. Se o Android aceitar a solicitação, o app limpa a chave de sessão em memória, cancela um prompt biométrico pendente e encerra a tarefa.
6. Na próxima abertura, o arquivo `keysafe_master_v1.bin` não existe e `MasterPasswordViewModel` identifica `AuthMode.CREATE`, exibindo a criação da senha mestra.

## Dados removidos pelo Android

A API oficial limpa os dados privados do app e a área privada do armazenamento externo, incluindo o banco Room (`keysafe_credentials.db`), arquivos em `noBackupFilesDir` (senha mestra e blob biométrico), SharedPreferences/DataStore, arquivos locais e cache. A limpeza do Android também remove entradas Android Keystore vinculadas ao UID do aplicativo. O APK continua instalado; dados OBB não são removidos (este projeto não usa OBB).

Não é necessário apagar manualmente o Room nem chamar `BiometricVaultStore.disable()`: a limpeza do usuário é mais abrangente e o Android remove os dados/Keystore vinculados ao aplicativo.

## Instalação

1. Faça commit ou backup do estado atual.
2. Extraia o ZIP.
3. Copie a pasta `app/src/main/java/com/example/keysafe/` para a mesma localização do projeto, substituindo os três arquivos indicados.
4. Faça Gradle Sync e Build no Android Studio.

## Testes manuais

- [ ] Abra o KeySafe, crie uma senha mestra e cadastre credenciais fictícias.
- [ ] Ative biometria, se disponível no dispositivo.
- [ ] Abra Configurações e pressione o botão de autodestruição.
- [ ] Feche o diálogo: nenhum dado deve ser removido.
- [ ] Reabra o diálogo e digite algo diferente de `APAGAR`: a confirmação deve permanecer desabilitada.
- [ ] Digite exatamente `APAGAR` e confirme.
- [ ] O aplicativo deve encerrar a tarefa.
- [ ] Abra o KeySafe novamente: deve aparecer a criação de senha mestra, como no primeiro uso.
- [ ] Confirme que as credenciais antigas não aparecem e que a biometria precisa ser configurada novamente.

A API retorna `true` quando o Android aceita a solicitação de limpeza e `false` quando não consegue iniciá-la. Se retornar `false`, o app permanece aberto e mostra um erro; a sessão não é bloqueada antes da confirmação de que a solicitação foi aceita.

## Commit sugerido

```bash
git add app/src/main/java/com/example/keysafe/MainActivity.kt \
        app/src/main/java/com/example/keysafe/navigation/AppNavigation.kt \
        app/src/main/java/com/example/keysafe/ui/screens/settings/SettingsScreen.kt

git diff --check
git commit -m "feat(security): add full app data wipe and factory reset"
```

## Limites

- A limpeza não desinstala o APK nem remove arquivos que o usuário tenha exportado/copiad​o para diretórios públicos ou outros aplicativos.
- A operação é irreversível. Não existe restauração automática após a limpeza.
- Este pacote foi preparado a partir dos arquivos da implementação biométrica anterior; a compilação Android completa e a limpeza real precisam ser verificadas no Android Studio/dispositivo.
