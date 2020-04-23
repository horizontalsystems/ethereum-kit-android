package io.horizontalsystems.ethereumkit.spv.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM BlockHeader WHERE height BETWEEN :startHeight AND :endHeight ORDER BY height ASC")
    fun getByBlockHeightRange(startHeight: Long, endHeight: Long): List<BlockHeader>
}
