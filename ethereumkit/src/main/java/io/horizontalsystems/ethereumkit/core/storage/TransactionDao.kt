package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction)

    @Query("SELECT hash FROM `Transaction`")
    fun getTransactionHashes(): List<ByteArray>

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` WHERE hash=:hash")
    fun getTransaction(hash: ByteArray): FullTransaction?

    @Query("SELECT MAX(syncOrder) FROM `Transaction`")
    fun getLastTransactionSyncOrder(): Long?

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` WHERE hash IN (:hashes)")
    fun getTransactions(hashes: List<ByteArray>): List<FullTransaction>

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` ORDER BY timestamp DESC")
    fun getTransactions(): List<FullTransaction>

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` WHERE syncOrder > :fromSyncOrder ORDER BY syncOrder ASC")
    fun getTransactions(fromSyncOrder: Long): List<FullTransaction>

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` ORDER BY timestamp DESC")
    fun getTransactionsAsync(): Single<List<FullTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactionReceipt: TransactionReceipt)

    @Query("SELECT * FROM TransactionReceipt WHERE transactionHash=:transactionHash")
    fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(logs: List<TransactionLog>)

    @Query("SELECT blockNumber FROM InternalTransaction ORDER BY blockNumber DESC LIMIT 1")
    fun getLastInternalTransactionBlockNumber(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInternalTransactions(internalTransactions: List<InternalTransaction>)

    @androidx.room.Transaction
    @Query("""
        SELECT tx.* 
            FROM `Transaction` as tx
            LEFT JOIN TransactionReceipt as receipt
            ON tx.hash == receipt.transactionHash 
            WHERE receipt.blockHash IS NULL 
            ORDER BY tx.nonce
            LIMIT 1
            """)
    fun getFirstPendingTransaction(): Transaction?

}
