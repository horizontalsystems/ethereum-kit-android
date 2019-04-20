package io.horizontalsystems.erc20kit.models

import android.arch.persistence.room.Entity
import java.math.BigInteger

@Entity(primaryKeys = ["contractAddress"])
class TokenBalance( val contractAddress: ByteArray,
                    val value: BigInteger,
                    val blockHeight: Long)
