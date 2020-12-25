package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(transaction: Transaction)

    @Query("SELECT hash FROM `Transaction`")
    fun getTransactionHashes(): List<ByteArray>

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` WHERE hash IN (:hashes)")
    fun getTransactions(hashes: List<ByteArray>): List<FullTransaction>

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction`")
    fun getTransactionsAsync(): Single<List<FullTransaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(transactionReceipt: TransactionReceipt)

    @Query("SELECT * FROM TransactionReceipt WHERE transactionHash=:transactionHash")
    fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(logs: List<TransactionLog>)

    @Query("SELECT blockNumber FROM InternalTransaction ORDER BY blockNumber DESC LIMIT 1")
    fun getLastInternalTransactionBlockNumber(): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertInternalTransactions(internalTransactions: List<InternalTransaction>)

}
