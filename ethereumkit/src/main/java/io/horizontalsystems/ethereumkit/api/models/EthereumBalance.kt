package io.horizontalsystems.ethereumkit.api.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigInteger

@Entity
class EthereumBalance(val balance: BigInteger, @PrimaryKey val id: String = "")
