package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.ethereumkit.core.storage.DatabaseConverters
import java.math.BigDecimal

@Entity
@TypeConverters(DatabaseConverters::class)
data class GasPriceRoom(@PrimaryKey val gasPriceInGwei: BigDecimal)
