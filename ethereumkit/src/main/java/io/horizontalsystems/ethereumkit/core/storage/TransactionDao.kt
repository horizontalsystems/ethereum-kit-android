package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.*
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.TransactionWithInternal
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ethereumTransactions: List<EthereumTransaction>)

    @Transaction
    @Query("SELECT * FROM EthereumTransaction ORDER BY timestamp DESC")
    fun getTransactions(): Single<List<TransactionWithInternal>>

    @Query("SELECT * FROM EthereumTransaction ORDER BY blockNumber DESC LIMIT 1")
    fun getLastTransaction(): EthereumTransaction?

    @Query("SELECT * FROM InternalTransaction ORDER BY blockNumber DESC LIMIT 1")
    fun getLastInternalTransaction(): InternalTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInternal(internalTransactions: List<InternalTransaction>)
}
