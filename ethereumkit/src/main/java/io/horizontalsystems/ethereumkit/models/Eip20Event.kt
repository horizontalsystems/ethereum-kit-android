package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger

@Entity
class Eip20Event(
    val hash: ByteArray,
    val blockNumber: Long,
    val contractAddress: Address,
    val from: Address,
    val to: Address,
    val value: BigInteger,

    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimal: Int,

    @PrimaryKey(autoGenerate = true) val id: Long = 0
) {

    @delegate:Ignore
    val hashString: String by lazy {
        hash.toHexString()
    }

}
