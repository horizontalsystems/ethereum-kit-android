package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ethereumTransactions: List<EthereumTransaction>)

    @Query("SELECT * FROM EthereumTransaction WHERE contractAddress = '' AND input = '0x' ORDER BY timeStamp DESC")
    fun getTransactions(): Single<List<EthereumTransaction>>

    @Query("SELECT * FROM EthereumTransaction WHERE contractAddress = :contractAddress ORDER BY timeStamp DESC")
    fun getErc20Transactions(contractAddress: String): Single<List<EthereumTransaction>>

    @Query("SELECT * FROM EthereumTransaction WHERE contractAddress = '' ORDER BY blockNumber DESC LIMIT 1")
    fun getLastTransaction(): EthereumTransaction?

    @Query("SELECT * FROM EthereumTransaction WHERE contractAddress != '' ORDER BY blockNumber DESC LIMIT 1")
    fun getTokenLastTransaction(): EthereumTransaction?

    @Delete
    fun delete(ethereumTransaction: EthereumTransaction)

    @Query("DELETE FROM EthereumTransaction")
    fun deleteAll()
}
