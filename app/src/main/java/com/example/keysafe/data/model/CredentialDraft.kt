package com.example.keysafe.data.model

/** Dados em memória, nunca salvar diretamente no SQLite ou em logs. */
data class CredentialDraft(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
    val favorite: Boolean
)

/** Apenas metadados em memória para a Home; a senha nunca aparece nessa lista. */
data class CredentialSummary(
    val id: String,
    val title: String,
    val username: String,
    val url: String,
    val category: String,
    val favorite: Boolean,
    val updatedAt: Long
)
