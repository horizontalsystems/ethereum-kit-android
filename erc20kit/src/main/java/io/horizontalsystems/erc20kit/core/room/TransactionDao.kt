package io.horizontalsystems.erc20kit.core.room

import android.arch.persistence.room.*
import io.horizontalsystems.erc20kit.models.Transaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Query("SELECT * FROM `Transaction` ORDER BY blockNumber DESC, logIndex DESC LIMIT 1")
    fun getLastTransaction(): Transaction?

    @Query("SELECT * FROM `Transaction` WHERE contractAddress = :contractAddress ORDER BY blockNumber DESC, logIndex DESC LIMIT 1")
    fun getLastTransaction(contractAddress: ByteArray): Transaction?

    @Query("SELECT * FROM `Transaction` ORDER BY blockNumber DESC, logIndex DESC")
    fun getAllTransactions(): Single<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<Transaction>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(transaction: Transaction)

    @Query("DELETE FROM `Transaction`")
    fun deleteAll()

}
