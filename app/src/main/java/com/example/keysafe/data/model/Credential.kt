package com.example.keysafe.data.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDateTime

@Entity (tableName = "credentials")
data class Credential (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val username: String,
    val encryptedPassword: String,
    val url: String,
    val notes: String,
    val category: String,
    val favorite: Boolean,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)