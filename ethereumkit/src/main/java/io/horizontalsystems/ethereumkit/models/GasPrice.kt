package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class GasPrice(val gasPriceInWei: Long, @PrimaryKey val id: String = "")
