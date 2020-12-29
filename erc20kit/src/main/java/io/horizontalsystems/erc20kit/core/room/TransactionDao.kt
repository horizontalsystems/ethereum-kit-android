package io.horizontalsystems.erc20kit.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.erc20kit.models.TransactionRecord
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Query("SELECT * FROM `TransactionRecord` ORDER BY timestamp DESC, interTransactionIndex DESC LIMIT 1")
    fun getLastTransaction(): TransactionRecord?

    @Query("SELECT * FROM `TransactionRecord` ORDER BY timestamp DESC, interTransactionIndex DESC, interTransactionIndex DESC")
    fun getAllTransactions(): Single<List<TransactionRecord>>

    @Query("SELECT * FROM `TransactionRecord` WHERE logIndex IS NULL")
    fun getPendingTransactions(): List<TransactionRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: TransactionRecord)

}
