package io.horizontalsystems.merkleiokit

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MerkleTransactionDao {

    @Query("SELECT * FROM MerkleTransactionHash")
    fun hashes() : List<MerkleTransactionHash>

    @Insert
    fun save(hash: MerkleTransactionHash)

    @Delete
    fun delete(hashes: List<MerkleTransactionHash>)

}
