package com.example.keysafe.security

import com.example.keysafe.data.model.Credential
import com.example.keysafe.data.model.CredentialDraft
import com.example.keysafe.data.model.CredentialSummary
import org.json.JSONObject
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra TODOS os campos da credencial com a chave AES-256 recuperada da
 * senha mestra. O id da linha é autenticado como AAD, impedindo que um
 * ciphertext de outra linha seja substituído silenciosamente.
 *
 * payload = [versão:1 byte] || [nonce:12 bytes] || [ciphertext + tag:16 bytes].
 */
class CredentialCrypto {
    private val random = SecureRandom()

    fun encrypt(draft: CredentialDraft, key: SecretKey): Credential {
        require(key.algorithm.equals("AES", ignoreCase = true))
        require(draft.title.isNotBlank()) { "Informe um título." }
        require(draft.password.isNotEmpty()) { "Informe a senha da credencial." }

        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("title", draft.title.trim())
            put("username", draft.username.trim())
            put("password", draft.password)
            put("url", draft.url.trim())
            put("notes", draft.notes)
            put("category", draft.category)
            put("favorite", draft.favorite)
            put("createdAt", now)
            put("updatedAt", now)
        }
        val plain = json.toString().toByteArray(Charsets.UTF_8)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(id))
            val ciphertext = cipher.doFinal(plain)
            Credential(id, byteArrayOf(VERSION) + nonce + ciphertext)
        } finally {
            plain.fill(0)
        }
    }

    /** Decifra para visualização; não mantém a senha na lista de resumos. */
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

    /**
     * Reservado à futura tela de detalhes/edição. Não colocar o resultado
     * em StateFlow de listagem nem mantê-lo após bloquear o cofre.
     */
    fun decryptDraft(entity: Credential, key: SecretKey): CredentialDraft {
        val json = decryptJson(entity, key)
        return CredentialDraft(
            title = json.getString("title"),
            username = json.getString("username"),
            password = json.getString("password"),
            url = json.getString("url"),
            notes = json.getString("notes"),
            category = json.getString("category"),
            favorite = json.getBoolean("favorite")
        )
    }

    private fun decryptJson(entity: Credential, key: SecretKey): JSONObject {
        val payload = entity.encryptedPayload
        require(payload.size >= HEADER_BYTES + 16 && payload[0] == VERSION) {
            "Formato da credencial inválido ou não suportado."
        }
        val nonce = payload.copyOfRange(1, HEADER_BYTES)
        val ciphertext = payload.copyOfRange(HEADER_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad(entity.id))
        val plain = cipher.doFinal(ciphertext) // tag inválida -> falha (não exibe dados)
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
        const val TAG_BITS = 128
        const val HEADER_BYTES = 1 + NONCE_BYTES
    }
}
