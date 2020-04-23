package io.horizontalsystems.ethereumkit.api.storage

import androidx.room.*
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight

@Dao
interface LastBlockHeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lastBlockHeight: LastBlockHeight)

    @Query("SELECT * FROM LastBlockHeight")
    fun getLastBlockHeight(): LastBlockHeight?

    @Delete
    fun delete(lastBlockHeight: LastBlockHeight)

    @Query("DELETE FROM LastBlockHeight")
    fun deleteAll()
}
