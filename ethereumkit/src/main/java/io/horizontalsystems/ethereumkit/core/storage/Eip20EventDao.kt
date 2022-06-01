package io.horizontalsystems.ethereumkit.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.horizontalsystems.ethereumkit.models.Eip20Event

@Dao
interface Eip20EventDao {

    @Query("SELECT * FROM Eip20Event ORDER BY blockNumber DESC LIMIT 1")
    fun getLastEip20Event(): Eip20Event?

    @Insert
    fun insertEip20Events(events: List<Eip20Event>)

    @Query("SELECT * FROM Eip20Event")
    fun getEip20Events(): List<Eip20Event>

    @Query("SELECT * FROM Eip20Event WHERE hash IN (:hashes)")
    fun getEip20EventsByHashes(hashes: List<ByteArray>): List<Eip20Event>

}
