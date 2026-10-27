package com.example.keysafe.data.repository

import com.example.keysafe.data.local.CredentialDao
import com.example.keysafe.data.model.Credential
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.data.model.CredentialSummary
import com.example.keysafe.security.CredentialCrypto
import kotlinx.coroutines.flow.Flow
import javax.crypto.SecretKey

class CredentialRepository(private val dao: CredentialDao) {
    private val crypto = CredentialCrypto()

    // O fluxo do Room contém APENAS bytes criptografados.
    fun observeEncrypted(): Flow<List<Credential>> = dao.observeAll()

    // A chave só é recebida enquanto a senha mestra desbloqueou o cofre.
    fun encrypt(draft: CredentialDraft, key: SecretKey): Credential =
        crypto.encrypt(draft, key)

    fun decryptSummary(credential: Credential, key: SecretKey): CredentialSummary =
        crypto.decryptSummary(credential, key)

    suspend fun insertEncrypted(credential: Credential) = dao.insert(credential)
}
