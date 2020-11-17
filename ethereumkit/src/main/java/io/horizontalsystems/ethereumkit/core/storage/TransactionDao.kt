package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionWithInternal
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<Transaction>)

    @androidx.room.Transaction
    @Query("SELECT * FROM `Transaction` ORDER BY timestamp DESC")
    fun getTransactions(): Single<List<TransactionWithInternal>>

    @Query("SELECT * FROM `Transaction` ORDER BY blockNumber DESC LIMIT 1")
    fun getLastTransaction(): Transaction?

    @Query("SELECT * FROM InternalTransaction ORDER BY blockNumber DESC LIMIT 1")
    fun getLastInternalTransaction(): InternalTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInternal(internalTransaction: InternalTransaction)
}
