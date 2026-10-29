package com.example.keysafe.security

import com.example.keysafe.data.model.Credential
import com.example.keysafe.data.model.CredentialDetails
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.data.model.CredentialSummary
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM: [versão:1] + [nonce aleatório:12] + [ciphertext autenticado com tag:16].
 * O UUID é associado ao ciphertext via AAD. A edição PRESERVA o UUID e o createdAt,
 * mas sempre gera um nonce aleatório novo, mesmo com a mesma chave.
 */
class CredentialCrypto {
    private val random = SecureRandom()

    fun encrypt(draft: CredentialDraft, key: SecretKey): Credential {
        val now = System.currentTimeMillis()
        return encryptRecord(UUID.randomUUID().toString(), draft, now, now, key)
    }

    fun update(existing: Credential, draft: CredentialDraft, key: SecretKey): Credential {
        // Autentica a credencial atual antes de modificá-la; não sobrescrever dados adulterados.
        val original = decryptJson(existing, key)
        val createdAt = original.getLong("createdAt")
        val updatedAt = maxOf(System.currentTimeMillis(), original.getLong("updatedAt") + 1L)
        return encryptRecord(existing.id, draft, createdAt, updatedAt, key)
    }

    fun decryptSummary(entity: Credential, key: SecretKey): CredentialSummary {
        val json = decryptJson(entity, key)
        return CredentialSummary(
            id = entity.id,
            title = json.getString("title"),
            username = json.getString("username"),
            url = json.getString("url"),
            category = json.getString("category"),
            favorite = json.getBoolean("favorite"),
            updatedAt = json.getLong("updatedAt")
        )
    }

    fun decryptDraft(entity: Credential, key: SecretKey): CredentialDraft =
        parseDraft(decryptJson(entity, key))

    fun decryptDetails(entity: Credential, key: SecretKey): CredentialDetails {
        val json = decryptJson(entity, key)
        return CredentialDetails(
            id = entity.id,
            draft = parseDraft(json),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt")
        )
    }

    private fun parseDraft(json: JSONObject) = CredentialDraft(
        title = json.getString("title"),
        username = json.getString("username"),
        password = json.getString("password"),
        url = json.getString("url"),
        notes = json.getString("notes"),
        category = json.getString("category"),
        favorite = json.getBoolean("favorite")
    )

    private fun encryptRecord(
        id: String,
        draft: CredentialDraft,
        createdAt: Long,
        updatedAt: Long,
        key: SecretKey
    ): Credential {
        require(key.algorithm.equals("AES", ignoreCase = true))
        require(draft.title.isNotBlank()) { "Informe um título." }
        require(draft.password.isNotEmpty()) { "Informe a senha da credencial." }

        val json = JSONObject().apply {
            put("title", draft.title.trim())
            put("username", draft.username.trim())
            put("password", draft.password)
            put("url", draft.url.trim())
            put("notes", draft.notes)
            put("category", draft.category)
            put("favorite", draft.favorite)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
        val plain = json.toString().toByteArray(Charsets.UTF_8)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(id))
            Credential(id, byteArrayOf(VERSION) + nonce + cipher.doFinal(plain))
        } finally {
            plain.fill(0)
        }
    }

    private fun decryptJson(entity: Credential, key: SecretKey): JSONObject {
        val payload = entity.encryptedPayload
        require(payload.size >= HEADER_BYTES + TAG_BYTES && payload[0] == VERSION) {
            "Formato de credencial inválido ou não suportado."
        }
        val nonce = payload.copyOfRange(1, HEADER_BYTES)
        val ciphertext = payload.copyOfRange(HEADER_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad(entity.id))
        val plain = cipher.doFinal(ciphertext) // tag inválida = erro, sem plaintext
        return try {
            JSONObject(String(plain, Charsets.UTF_8))
        } finally {
            plain.fill(0)
        }
    }

    private fun aad(id: String): ByteArray =
        "KeySafe:credential:v1:$id".toByteArray(Charsets.UTF_8)

    private companion object {
        const val VERSION: Byte = 1
        const val NONCE_BYTES = 12
        const val TAG_BYTES = 16
        const val TAG_BITS = TAG_BYTES * 8
        const val HEADER_BYTES = 1 + NONCE_BYTES
    }
}
