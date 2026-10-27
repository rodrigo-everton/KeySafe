package com.example.keysafe.security

import com.example.keysafe.data.model.Credential
import com.example.keysafe.data.model.CredentialDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

/** Executar no dispositivo/emulador Android (org.json do framework). */
class CredentialCryptoTest {
    private val crypto = CredentialCrypto()

    private fun aesKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val draft = CredentialDraft(
        title = "Conta de teste",
        username = "usuario@exemplo.com",
        password = "S3gredo-teste!",
        url = "https://exemplo.com",
        notes = "Anotações privadas",
        category = "Trabalho",
        favorite = true
    )

    @Test
    fun roundTripAndNoPlaintext() {
        val key = aesKey()
        val one = crypto.encrypt(draft, key)
        val two = crypto.encrypt(draft, key)
        assertNotEquals(one.id, two.id)
        assertFalse(one.encryptedPayload.toString(Charsets.ISO_8859_1).contains(draft.password))
        assertFalse(one.encryptedPayload.toString(Charsets.ISO_8859_1).contains(draft.username))
        assertEquals(draft, crypto.decryptDraft(one, key))
        assertEquals(draft.title, crypto.decryptSummary(one, key).title)
    }

    @Test
    fun wrongKeyFailsAuthentication() {
        val credential = crypto.encrypt(draft, aesKey())
        assertThrows(AEADBadTagException::class.java) {
            crypto.decryptDraft(credential, aesKey())
        }
    }

    @Test
    fun tamperFailsAuthentication() {
        val key = aesKey()
        val credential = crypto.encrypt(draft, key)
        val tampered = credential.encryptedPayload.clone().also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }
        assertThrows(AEADBadTagException::class.java) {
            crypto.decryptDraft(credential.copy(encryptedPayload = tampered), key)
        }
    }

    @Test
    fun ciphertextCannotBeMovedToAnotherId() {
        val key = aesKey()
        val credential = crypto.encrypt(draft, key)
        val moved = Credential(UUID.randomUUID().toString(), credential.encryptedPayload)
        assertThrows(AEADBadTagException::class.java) {
            crypto.decryptDraft(moved, key)
        }
    }
}
