package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class EthereumBalance(
        @PrimaryKey
        val address: String,
        val balance: String
)
