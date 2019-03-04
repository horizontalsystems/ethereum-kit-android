package io.horizontalsystems.ethereumkit.spv.core.room

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

@Dao
interface BlockHeaderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(blockHeader: BlockHeader)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(blockHeaders: List<BlockHeader>)

    @Query("SELECT * FROM BlockHeader WHERE hashHex=:hashHex")
    fun getByHashHex(hashHex: ByteArray): BlockHeader

    @Query("SELECT * FROM BlockHeader ORDER BY height DESC")
    fun getAll(): List<BlockHeader>
}
