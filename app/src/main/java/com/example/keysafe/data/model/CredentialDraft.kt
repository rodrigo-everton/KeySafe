package com.example.keysafe.data.model

/** Usado somente na memória da sessão desbloqueada; nunca persistir como JSON aberto. */
data class CredentialDraft(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
    val favorite: Boolean
)

/** Resumo em memória para a lista, sem a senha. */
data class CredentialSummary(
    val id: String,
    val title: String,
    val username: String,
    val url: String,
    val category: String,
    val favorite: Boolean,
    val updatedAt: Long
)

/** Detalhes decifrados somente durante uma sessão ativa. */
data class CredentialDetails(
    val id: String,
    val draft: CredentialDraft,
    val createdAt: Long,
    val updatedAt: Long
)
