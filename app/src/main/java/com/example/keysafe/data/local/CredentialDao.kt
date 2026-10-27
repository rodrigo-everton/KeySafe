package com.example.keysafe.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.keysafe.data.model.Credential
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(credential: Credential)

    @Query("SELECT * FROM credentials")
    fun observeAll(): Flow<List<Credential>>
}
