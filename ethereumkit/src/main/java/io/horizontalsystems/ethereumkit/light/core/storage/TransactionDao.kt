package io.horizontalsystems.ethereumkit.light.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Query("SELECT * FROM EthereumTransaction WHERE contractAddress = '' AND input = '0x' ORDER BY timeStamp DESC")
    fun getTransactions(): Single<List<EthereumTransaction>>
}
