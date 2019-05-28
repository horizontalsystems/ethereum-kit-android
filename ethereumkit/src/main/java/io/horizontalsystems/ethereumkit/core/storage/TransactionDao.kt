package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
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

    @Query("SELECT * FROM EthereumTransaction ORDER BY blockNumber DESC LIMIT 1")
    fun getLastTransaction(): EthereumTransaction?

}
