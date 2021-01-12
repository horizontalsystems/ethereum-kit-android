package io.horizontalsystems.erc20kit.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.erc20kit.models.TransactionSyncOrder

@Dao
interface TransactionSyncOrderDao {

    @Query("SELECT * FROM TransactionSyncOrder LIMIT 1")
    fun getTransactionSyncOrder(): TransactionSyncOrder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(syncOrder: TransactionSyncOrder)

}
