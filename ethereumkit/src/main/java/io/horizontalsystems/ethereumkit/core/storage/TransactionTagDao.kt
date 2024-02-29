package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.TransactionTag

@Dao
interface TransactionTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tags: List<TransactionTag>)

    @Query("SELECT DISTINCT name FROM TransactionTag WHERE name LIKE '%_outgoing' OR name LIKE '%_incoming'")
    fun getDistinctTokenContractAddresses(): List<String>

}
