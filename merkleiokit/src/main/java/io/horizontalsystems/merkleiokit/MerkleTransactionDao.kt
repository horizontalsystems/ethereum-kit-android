package io.horizontalsystems.merkleiokit

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MerkleTransactionDao {

    @Query("SELECT * FROM MerkleTransactionHash WHERE chainId = :chainId")
    fun hashes(chainId: Int) : List<MerkleTransactionHash>

    @Insert
    fun save(hash: MerkleTransactionHash)

    @Query("SELECT COUNT(*) > 0 FROM MerkleTransactionHash WHERE chainId = :chainId")
    fun hasMerkleTransactions(chainId: Int) : Boolean

    @Delete
    fun delete(hashes: List<MerkleTransactionHash>): Any

}
