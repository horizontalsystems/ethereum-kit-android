package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class GasPrice(val lowPriority: Long, val mediumPriority: Long, val highPriority: Long, val date: Long) {

    @PrimaryKey
    var primaryKey: String = "primary-key"

    companion object {
        val defaultGasPrice = GasPrice(
                lowPriority = 1_000_000_000,
                mediumPriority = 3_000_000_000,
                highPriority = 9_000_000_000,
                date = 1543211299660
        )
    }
}
