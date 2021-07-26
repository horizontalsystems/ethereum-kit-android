package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.models.Transaction
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

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` WHERE hash IN (:hashes)")
    fun getTransactions(hashes: List<ByteArray>): List<FullTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactionReceipt: TransactionReceipt)

    @Query("SELECT * FROM TransactionReceipt WHERE transactionHash=:transactionHash")
    fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(logs: List<TransactionLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInternalTransactions(internalTransactions: List<InternalTransaction>)

    @androidx.room.Transaction
    @Query("""
        SELECT tx.* 
            FROM `Transaction` as tx
            LEFT JOIN TransactionReceipt as receipt
            ON tx.hash == receipt.transactionHash
            LEFT JOIN DroppedTransaction as droppedTx
            ON tx.hash == droppedTx.hash
            WHERE receipt.transactionHash IS NULL AND droppedTx.hash IS NULL
            ORDER BY tx.nonce, tx.timestamp
            """)
    fun getPendingTransactions(): List<Transaction>

    @androidx.room.Transaction
    @Query("""
        SELECT tx.* 
            FROM `Transaction` as tx
            LEFT JOIN TransactionReceipt as receipt
            ON tx.hash == receipt.transactionHash
            WHERE receipt.transactionHash IS NULL AND tx.nonce = :nonce
            LIMIT 1
            """)
    fun getPendingTransaction(nonce: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(droppedTransaction: DroppedTransaction)

    @RawQuery
    fun getPending(query: SupportSQLiteQuery): List<FullTransaction>

    @RawQuery
    fun getTransactionsBeforeAsync(query: SupportSQLiteQuery): Single<List<FullTransaction>>

}
