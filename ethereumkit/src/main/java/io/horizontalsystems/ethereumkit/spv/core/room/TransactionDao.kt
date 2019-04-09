package io.horizontalsystems.ethereumkit.spv.core.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Query("SELECT * FROM EthereumTransaction WHERE input = '0x' ORDER BY timestamp DESC")
    fun getTransactions(): Single<List<EthereumTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<EthereumTransaction>)
}
