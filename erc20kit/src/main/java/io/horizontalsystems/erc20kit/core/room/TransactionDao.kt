package io.horizontalsystems.erc20kit.core.room

import androidx.room.*
import io.horizontalsystems.erc20kit.models.Transaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Query("SELECT * FROM `Transaction` ORDER BY blockNumber DESC, logIndex DESC LIMIT 1")
    fun getLastTransaction(): Transaction?

    @Query("SELECT * FROM `Transaction` ORDER BY timestamp DESC, transactionIndex DESC, interTransactionIndex DESC")
    fun getAllTransactions(): Single<List<Transaction>>

    @Query("SELECT * FROM `Transaction` WHERE blockNumber IS NULL")
    fun getPendingTransactions(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<Transaction>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(transaction: Transaction)

}
