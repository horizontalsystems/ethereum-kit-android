package io.horizontalsystems.nftkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

@Entity
data class Eip721Event(
    val hash: ByteArray,
    val blockNumber: Long,
    val contractAddress: Address,
    val from: Address,
    val to: Address,
    val tokenId: BigInteger,
    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimal: Int,

    @PrimaryKey(autoGenerate = true) val id: Long = 0
)