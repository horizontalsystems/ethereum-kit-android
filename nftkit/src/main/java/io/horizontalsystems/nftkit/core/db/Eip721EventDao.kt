package io.horizontalsystems.nftkit.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.nftkit.models.Eip721Event

@Dao
interface Eip721EventDao {

    @Query("SELECT * FROM Eip721Event ORDER BY blockNumber DESC LIMIT 1")
    fun lastEvent(): Eip721Event?

    @Query("SELECT * FROM Eip721Event")
    fun events(): List<Eip721Event>

    @Query("SELECT * FROM Eip721Event WHERE hash IN (:hashes)")
    fun eventsByHash(hashes: List<ByteArray>): List<Eip721Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Eip721Event>)
}