package com.example.keysafe.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.example.keysafe.data.model.Credential

/**
 * Esquema inicial do cofre criptografado. Não usar fallbackToDestructiveMigration:
 * uma migração destrutiva apagaria as credenciais do usuário.
 */
@Database(entities = [Credential::class], version = 1, exportSchema = false)
abstract class CredentialDatabase : RoomDatabase() {

    abstract fun credentialDao(): CredentialDao

    companion object {
        @Volatile private var instance: CredentialDatabase? = null

        fun getInstance(context: Context): CredentialDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CredentialDatabase::class.java,
                    "keysafe_credentials.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { instance = it }
            }
    }
}
