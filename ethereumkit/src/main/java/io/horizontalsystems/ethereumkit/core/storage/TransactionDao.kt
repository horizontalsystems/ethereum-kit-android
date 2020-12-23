package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(transaction: Transaction)

    @Query("SELECT hash FROM `Transaction`")
    fun getTransactionHashes(): List<ByteArray>

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insert(transactions: List<EtherscanTransaction>)
//
//    @androidx.room.Transaction
//    @Query("SELECT * FROM EtherscanTransaction ORDER BY timestamp DESC")
//    fun getTransactions(): Single<List<TransactionWithInternal>>
//
//    @Query("SELECT * FROM EtherscanTransaction ORDER BY blockNumber DESC LIMIT 1")
//    fun getLastTransaction(): EtherscanTransaction?
//
//    @Query("SELECT * FROM InternalTransaction ORDER BY blockNumber DESC LIMIT 1")
//    fun getLastInternalTransaction(): InternalTransaction?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertInternal(internalTransaction: InternalTransaction)
}
