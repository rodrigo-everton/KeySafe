package com.example.keysafe.data.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * ÚNICA entidade persistida no Room. Todo o conteúdo sensível é armazenado
 * como ciphertext autenticado (versão + nonce + AES-GCM ciphertext/tag).
 * O id aleatório não contém dados pessoais.
 */
@Entity(tableName = "credentials")
data class Credential(
    @PrimaryKey val id: String,
    val encryptedPayload: ByteArray
)
