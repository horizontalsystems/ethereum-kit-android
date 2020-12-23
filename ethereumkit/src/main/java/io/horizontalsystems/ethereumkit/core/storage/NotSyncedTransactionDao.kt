package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.NotSyncTransactionRecord
import io.reactivex.Single

@Dao
interface NotSyncedTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: NotSyncTransactionRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactions: List<NotSyncTransactionRecord>)

    @Query("SELECT * FROM NotSyncTransactionRecord LIMIT :limit")
    fun get(limit: Int): List<NotSyncTransactionRecord>

    @Query("DELETE FROM NotSyncTransactionRecord WHERE hash = :hash")
    fun deleteByHash(hash: ByteArray)

}
