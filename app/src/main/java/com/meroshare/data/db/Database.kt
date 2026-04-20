package com.meroshare.data.db

import androidx.room.*
import com.meroshare.data.model.Account
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

@Database(entities = [Account::class], version = 1, exportSchema = false)
abstract class MeroShareDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
