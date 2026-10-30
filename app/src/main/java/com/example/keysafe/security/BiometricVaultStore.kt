package com.example.keysafe.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Via ALTERNATIVA de abrir a mesma chave aleatória do cofre.
 *
 * - A senha mestra permanece como método primário/recuperação.
 * - A chave Keystore só pode ser usada após BIOMETRIC_STRONG para CADA operação.
 * - O arquivo em noBackupFilesDir contém apenas IV + chave do cofre cifrada.
 * - Não contém senha mestra, modelo biométrico ou chave Keystore exportável.
 *
 * Fluxo de cadastro: beginEnrollment() -> BiometricPrompt(CryptoObject(cipher))
 *                    -> finishEnrollment(cipher autenticado, chave do cofre).
 * Fluxo de abertura: beginUnlock() -> BiometricPrompt(CryptoObject(cipher))
 *                   -> finishUnlock(cipher autenticado).
 */
class BiometricVaultStore(context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "keysafe_biometric_v1.bin"))

    /** A existência do arquivo NÃO significa que a biometria ainda seja válida. */
    fun isConfigured(): Boolean = file.baseFile.exists()

    /** Requer BIOMETRIC_STRONG disponível e cofre aberto por senha mestra. */
    @Synchronized
    fun beginEnrollment(): Cipher {
        check(!isConfigured()) { "Desative a biometria atual antes de reconfigurar." }
        // Caso uma tentativa anterior tenha sido cancelada, a chave órfã é descartada.
        if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val specBuilder = KeyGenParameterSpec.Builder(
            ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            specBuilder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        generator.init(specBuilder.build())
        val key = generator.generateKey()
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    /** Use SOMENTE o Cipher devolvido em AuthenticationResult.cryptoObject. */
    @Synchronized
    fun finishEnrollment(authenticatedCipher: Cipher, vaultKey: ByteArray) {
        check(!isConfigured()) { "A biometria já está configurada." }
        require(vaultKey.size == KEY_SIZE) { "Chave do cofre inválida." }
        val iv = authenticatedCipher.iv
        require(iv.size == IV_SIZE) { "IV criptográfico inválido." }
        authenticatedCipher.updateAAD(AAD)
        val ciphertext = authenticatedCipher.doFinal(vaultKey)
        check(ciphertext.size == WRAPPED_SIZE) { "Tamanho do registro inválido." }
        writeRecord(Record(iv, ciphertext))
    }

    /** Pode falhar se a biometria foi alterada, o bloqueio foi removido ou o dado está corrompido. */
    @Synchronized
    fun beginUnlock(): Cipher {
        val record = readRecord() ?: error("Biometria não configurada.")
        val key = keyStore.getKey(ALIAS, null) as? SecretKey
            ?: error("Chave biométrica não encontrada. Reconfigure com a senha mestra.")
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, record.iv))
        }
    }

    /** Só funciona se o Android autorizou o Cipher pelo BiometricPrompt. */
    @Synchronized
    fun finishUnlock(authenticatedCipher: Cipher): ByteArray {
        val record = readRecord() ?: error("Biometria não configurada.")
        authenticatedCipher.updateAAD(AAD)
        val plainKey = authenticatedCipher.doFinal(record.ciphertext)
        if (plainKey.size != KEY_SIZE) {
            plainKey.fill(0)
            error("Chave do cofre inválida.")
        }
        return plainKey
    }

    /** Revoga o método biométrico; não altera a chave mestra nem o banco Room. */
    @Synchronized
    fun disable() {
        // Ao excluir o blob primeiro, uma interrupção não deixará uma opção habilitada.
        file.delete()
        if (keyStore.containsAlias(ALIAS)) keyStore.deleteEntry(ALIAS)
    }

    private fun readRecord(): Record? {
        val stream = try {
            file.openRead()
        } catch (_: FileNotFoundException) {
            return null
        }
        return DataInputStream(stream).use { input ->
            check(input.readInt() == MAGIC) { "Cabeçalho biométrico inválido." }
            check(input.readInt() == VERSION) { "Versão biométrica desconhecida." }
            val iv = ByteArray(IV_SIZE).also(input::readFully)
            val wrapped = ByteArray(WRAPPED_SIZE).also(input::readFully)
            check(input.read() == -1) { "Registro biométrico excedente." }
            Record(iv, wrapped)
        }
    }

    private fun writeRecord(record: Record) {
        val stream = file.startWrite()
        try {
            DataOutputStream(stream).apply {
                writeInt(MAGIC)
                writeInt(VERSION)
                write(record.iv)
                write(record.ciphertext)
                flush()
            }
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    private data class Record(val iv: ByteArray, val ciphertext: ByteArray)

    private companion object {
        const val ALIAS = "keysafe_biometric_wrap_v1"
        const val MAGIC = 0x4B534249 // KSBI
        const val VERSION = 1
        const val IV_SIZE = 12
        const val KEY_SIZE = 32
        const val WRAPPED_SIZE = KEY_SIZE + 16
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val AAD = "KeySafe:biometric-vault-key:v1".toByteArray(Charsets.UTF_8)
    }
}
