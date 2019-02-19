package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.TransactionRoom
import io.reactivex.Single

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: TransactionRoom)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<TransactionRoom>)

    @Query("SELECT * FROM TransactionRoom WHERE contractAddress = :address ORDER BY timeStamp DESC")
    fun getTransactions(address: String?): Single<List<TransactionRoom>>

    @Delete
    fun delete(transaction: TransactionRoom)

    @Query("DELETE FROM TransactionRoom")
    fun deleteAll()
}
