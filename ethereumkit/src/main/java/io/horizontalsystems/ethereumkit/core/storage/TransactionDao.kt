package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ethereumTransaction: EthereumTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ethereumTransactions: List<EthereumTransaction>)

    @Query("SELECT * FROM EthereumTransaction WHERE contractAddress = :address ORDER BY timeStamp DESC")
    fun getTransactions(address: String?): Single<List<EthereumTransaction>>

    @Delete
    fun delete(ethereumTransaction: EthereumTransaction)

    @Query("DELETE FROM EthereumTransaction")
    fun deleteAll()
}
