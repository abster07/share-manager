package com.share_manager.data.db

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.share_manager.data.model.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}

// ─────────────────────────────────────────────────────────────────────────────

@Database(
    entities  = [Account::class],
    version   = 2,           // bumped from 1 → 2 for new dp / password columns
    exportSchema = false     // set to true and configure room.schemaLocation if you want schema tracking
)
abstract class MeroShareDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    companion object {
        /**
         * Migration 1 → 2: adds the [Account.dp] and [Account.password] columns
         * that were introduced when apply / login support was added.
         *
         * Both columns default to an empty string so existing rows remain valid.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE accounts ADD COLUMN dp TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE accounts ADD COLUMN password TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
