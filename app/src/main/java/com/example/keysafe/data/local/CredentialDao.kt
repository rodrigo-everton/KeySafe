package com.example.keysafe.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.example.keysafe.data.model.Credential
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(credential: Credential)

    @Query("SELECT * FROM credentials")
    fun observeAll(): Flow<List<Credential>>

    @Query("SELECT * FROM credentials WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): Credential?

    // O @Update procura a linha pela chave primária (UUID).
    // Retorna zero se o registro já não existir.
    @Update
    suspend fun update(credential: Credential): Int

    // Deleta somente o UUID selecionado, nunca todos os registros.
    @Query("DELETE FROM credentials WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
