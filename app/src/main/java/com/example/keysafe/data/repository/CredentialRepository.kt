package com.example.keysafe.data.repository

import com.example.keysafe.data.local.CredentialDao
import com.example.keysafe.data.model.Credential
import com.example.keysafe.data.model.CredentialDetails
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.data.model.CredentialSummary
import com.example.keysafe.security.CredentialCrypto
import kotlinx.coroutines.flow.Flow
import javax.crypto.SecretKey

class CredentialRepository(private val dao: CredentialDao) {
    private val crypto = CredentialCrypto()

    fun observeEncrypted(): Flow<List<Credential>> = dao.observeAll()
    suspend fun findEncrypted(id: String): Credential? = dao.findById(id)

    fun encrypt(draft: CredentialDraft, key: SecretKey): Credential =
        crypto.encrypt(draft, key)

    fun encryptUpdate(existing: Credential, draft: CredentialDraft, key: SecretKey): Credential =
        crypto.update(existing, draft, key)

    fun decryptSummary(entity: Credential, key: SecretKey): CredentialSummary =
        crypto.decryptSummary(entity, key)

    fun decryptDetails(entity: Credential, key: SecretKey): CredentialDetails =
        crypto.decryptDetails(entity, key)

    suspend fun insertEncrypted(entity: Credential) = dao.insert(entity)
    suspend fun updateEncrypted(entity: Credential): Int = dao.update(entity)
    suspend fun deleteById(id: String): Int = dao.deleteById(id)
}
