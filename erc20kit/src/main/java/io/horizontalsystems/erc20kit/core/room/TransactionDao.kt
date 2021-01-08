package io.horizontalsystems.erc20kit.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.erc20kit.models.TransactionCache
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Query("SELECT * FROM TransactionCache ORDER BY timestamp DESC, interTransactionIndex DESC LIMIT 1")
    fun getLastTransaction(): TransactionCache?

    @Query("SELECT * FROM TransactionCache ORDER BY timestamp DESC, interTransactionIndex DESC, interTransactionIndex DESC")
    fun getAllTransactions(): Single<List<TransactionCache>>

    @Query("SELECT * FROM TransactionCache WHERE logIndex IS NULL")
    fun getPendingTransactions(): List<TransactionCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: TransactionCache)

}
