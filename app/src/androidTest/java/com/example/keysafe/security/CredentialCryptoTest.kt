package com.example.keysafe.security

import com.example.keysafe.data.model.Credential
import com.example.keysafe.data.model.CredentialDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator

/** Teste instrumentado: usa org.json do Android. */
class CredentialCryptoTest {
    private val crypto = CredentialCrypto()

    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private val draft = CredentialDraft(
        title = "Exemplo",
        username = "usuario@exemplo.com",
        password = "SenhaAntiga123!",
        url = "https://exemplo.com",
        notes = "Anotações privadas",
        category = "Trabalho",
        favorite = false
    )

    @Test
    fun roundTripAndSummaryHidePassword() {
        val secret = key()
        val record = crypto.encrypt(draft, secret)
        assertEquals(draft, crypto.decryptDraft(record, secret))
        assertEquals(draft.title, crypto.decryptSummary(record, secret).title)
        assertFalse(record.encryptedPayload.toString(Charsets.ISO_8859_1).contains(draft.password))
    }

    @Test
    fun updatePreservesIdAndCreatedAtAndChangesEncryptedPayload() {
        val secret = key()
        val initial = crypto.encrypt(draft, secret)
        val before = crypto.decryptDetails(initial, secret)
        val changed = draft.copy(
            title = "Exemplo editado",
            username = "outro@exemplo.com",
            password = "SenhaNova456!",
            notes = "Notas editadas",
            favorite = true
        )
        val updated = crypto.update(initial, changed, secret)
        val after = crypto.decryptDetails(updated, secret)

        assertEquals(initial.id, updated.id)
        assertEquals(before.createdAt, after.createdAt)
        assertTrue(after.updatedAt > before.updatedAt)
        assertEquals(changed, after.draft)
        assertFalse(initial.encryptedPayload.contentEquals(updated.encryptedPayload))
        assertEquals(draft, crypto.decryptDraft(initial, secret))
    }

    @Test
    fun repeatedEditsUseDifferentNonces() {
        val secret = key()
        val initial = crypto.encrypt(draft, secret)
        val edit1 = crypto.update(initial, draft, secret)
        val edit2 = crypto.update(edit1, draft, secret)
        assertNotEquals(
            edit1.encryptedPayload.sliceArray(1..12).toList(),
            edit2.encryptedPayload.sliceArray(1..12).toList()
        )
    }

    @Test
    fun incorrectKeyTamperingAndIdSwappingFail() {
        val secret = key()
        val initial = crypto.encrypt(draft, secret)
        assertThrows(AEADBadTagException::class.java) {
            crypto.decryptDetails(initial, key())
        }
        val altered = initial.copy(encryptedPayload = initial.encryptedPayload.clone().also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        })
        assertThrows(AEADBadTagException::class.java) {
            crypto.update(altered, draft, secret)
        }
        val swapped = Credential(UUID.randomUUID().toString(), initial.encryptedPayload)
        assertThrows(AEADBadTagException::class.java) {
            crypto.decryptDetails(swapped, secret)
        }
    }
}
