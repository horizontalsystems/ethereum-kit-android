package io.horizontalsystems.ethereumkit.api.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.math.BigInteger

@Entity
class EthereumBalance(val balance: BigInteger, @PrimaryKey val id: String = "")
