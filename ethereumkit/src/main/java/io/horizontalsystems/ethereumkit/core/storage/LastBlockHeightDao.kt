package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.*
import io.horizontalsystems.ethereumkit.models.LastBlockHeightRoom

@Dao
interface LastBlockHeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(lastBlockHeight: LastBlockHeightRoom)

    @Query("SELECT * FROM LastBlockHeightRoom")
    fun getLastBlockHeight(): LastBlockHeightRoom?

    @Delete
    fun delete(lastBlockHeight: LastBlockHeightRoom)

    @Query("DELETE FROM LastBlockHeightRoom")
    fun deleteAll()
}
