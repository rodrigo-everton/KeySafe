package com.example.keysafe.security

import android.content.Context
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A senha mestra NUNCA é salva. Ela deriva uma chave que protege a chave
 * aleatória de 256 bits do cofre. O arquivo fica em noBackupFilesDir.
 *
 * Formato v1: magic:int + version:int + salt[16] + nonce[12] + wrappedKey[48].
 * minSdk recomendado: 26+ (PBKDF2WithHmacSHA256).
 */
class MasterPasswordStore(context: Context) {

    enum class Status { NEEDS_SETUP, LOCKED, CORRUPTED }

    private val random = SecureRandom()
    private val file = AtomicFile(File(context.noBackupFilesDir, "keysafe_master_v1.bin"))

    fun status(): Status = try {
        if (readRecord() == null) Status.NEEDS_SETUP else Status.LOCKED
    } catch (_: Exception) {
        Status.CORRUPTED
    }

    /** Cria a chave do cofre uma única vez. Retorna a chave para esta sessão. */
    @Synchronized
    fun create(password: CharArray): ByteArray {
        require(password.size >= 12 && password.any { !it.isWhitespace() }) {
            "A senha mestra deve ter pelo menos 12 caracteres."
        }
        check(readRecord() == null) { "O cofre já foi configurado." }

        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)
        val vaultKey = ByteArray(KEY_SIZE).also(random::nextBytes)
        val derived = deriveKey(password, salt)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(AAD)
            val wrapped = cipher.doFinal(vaultKey)
            check(wrapped.size == WRAPPED_SIZE)
            writeRecord(Record(salt, nonce, wrapped))
            return vaultKey
        } catch (e: Exception) {
            vaultKey.fill(0)
            throw e
        } finally {
            derived.fill(0)
        }
    }

    /** Desbloqueia recuperando a chave do cofre; senha errada falha na tag GCM. */
    @Synchronized
    fun unlock(password: CharArray): ByteArray {
        val record = readRecord() ?: error("Cofre ainda não configurado.")
        val derived = deriveKey(password, record.salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(derived, "AES"),
                GCMParameterSpec(128, record.nonce)
            )
            cipher.updateAAD(AAD)
            return try {
                val key = cipher.doFinal(record.wrappedKey)
                if (key.size != KEY_SIZE) {
                    key.fill(0)
                    throw IllegalStateException("Chave do cofre inválida.")
                }
                key
            } catch (_: BadPaddingException) {
                throw IncorrectMasterPasswordException()
            }
        } finally {
            derived.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE * 8)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun readRecord(): Record? {
        val stream = try {
            file.openRead()
        } catch (_: FileNotFoundException) {
            return null
        }
        return DataInputStream(stream).use { input ->
            check(input.readInt() == MAGIC) { "Cabeçalho inválido." }
            check(input.readInt() == VERSION) { "Versão desconhecida." }
            val salt = ByteArray(SALT_SIZE).also(input::readFully)
            val nonce = ByteArray(NONCE_SIZE).also(input::readFully)
            val wrapped = ByteArray(WRAPPED_SIZE).also(input::readFully)
            check(input.read() == -1) { "Arquivo com dados excedentes." }
            Record(salt, nonce, wrapped)
        }
    }

    private fun writeRecord(record: Record) {
        val stream = file.startWrite()
        try {
            val output = DataOutputStream(stream)
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.write(record.salt)
            output.write(record.nonce)
            output.write(record.wrappedKey)
            output.flush()
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    private data class Record(
        val salt: ByteArray,
        val nonce: ByteArray,
        val wrappedKey: ByteArray
    )

    private companion object {
        const val MAGIC = 0x4B535646 // KSVF
        const val VERSION = 1
        const val ITERATIONS = 600_000
        const val SALT_SIZE = 16
        const val NONCE_SIZE = 12
        const val KEY_SIZE = 32
        const val WRAPPED_SIZE = KEY_SIZE + 16 // ciphertext + GCM tag
        val AAD = "KeySafe:wrapped-vault-key:v1".toByteArray(Charsets.UTF_8)
    }
}

class IncorrectMasterPasswordException : Exception("Senha mestra incorreta.")
