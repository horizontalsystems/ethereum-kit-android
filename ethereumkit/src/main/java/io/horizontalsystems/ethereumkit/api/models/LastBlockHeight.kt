package io.horizontalsystems.ethereumkit.api.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
class LastBlockHeight(val height: Int, @PrimaryKey val id: String = "")
