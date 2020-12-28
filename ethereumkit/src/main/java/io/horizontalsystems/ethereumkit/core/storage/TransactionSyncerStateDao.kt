package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.TransactionSyncerState

@Dao
interface TransactionSyncerStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transactionSyncerState: TransactionSyncerState)

    @Query("SELECT * FROM TransactionSyncerState WHERE id=:id")
    fun getTransactionSyncerState(id: String): TransactionSyncerState?

}
