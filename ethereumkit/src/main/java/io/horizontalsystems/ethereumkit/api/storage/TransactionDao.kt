package io.horizontalsystems.ethereumkit.api.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ethereumTransactions: List<EthereumTransaction>)

    @Query("SELECT * FROM EthereumTransaction WHERE length(input) = 0 ORDER BY timestamp DESC")
    fun getTransactions(): Single<List<EthereumTransaction>>

    @Query("SELECT * FROM EthereumTransaction WHERE `to` = :contractAddress ORDER BY timestamp DESC")
    fun getErc20Transactions(contractAddress: ByteArray): Single<List<EthereumTransaction>>

    @Query("SELECT * FROM EthereumTransaction WHERE length(input) = 0 ORDER BY blockNumber DESC LIMIT 1")
    fun getLastTransaction(): EthereumTransaction?

    @Query("SELECT * FROM EthereumTransaction WHERE length(input) > 0 ORDER BY blockNumber DESC LIMIT 1")
    fun getTokenLastTransaction(): EthereumTransaction?

    @Delete
    fun delete(ethereumTransaction: EthereumTransaction)

    @Query("DELETE FROM EthereumTransaction")
    fun deleteAll()
}
