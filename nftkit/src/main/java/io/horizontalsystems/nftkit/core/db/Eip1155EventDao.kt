package io.horizontalsystems.nftkit.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.nftkit.models.Eip1155Event

@Dao
interface Eip1155EventDao {

    @Query("SELECT * FROM Eip1155Event ORDER BY blockNumber DESC LIMIT 1")
    fun lastEvent(): Eip1155Event?

    @Query("SELECT * FROM Eip1155Event")
    fun events(): List<Eip1155Event>

    @Query("SELECT * FROM Eip1155Event WHERE hash IN (:hashes)")
    fun eventsByHash(hashes: List<ByteArray>): List<Eip1155Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Eip1155Event>)
}