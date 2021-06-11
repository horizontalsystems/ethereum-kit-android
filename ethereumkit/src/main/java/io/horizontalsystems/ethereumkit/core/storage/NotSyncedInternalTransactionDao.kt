package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.*
import io.horizontalsystems.ethereumkit.models.NotSyncedInternalTransaction

@Dao
interface NotSyncedInternalTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg notSyncedInternalTransaction: NotSyncedInternalTransaction)

    @Query("SELECT * FROM NotSyncedInternalTransaction")
    fun getAll(): List<NotSyncedInternalTransaction>

    @Delete
    fun delete(vararg item: NotSyncedInternalTransaction)

}
