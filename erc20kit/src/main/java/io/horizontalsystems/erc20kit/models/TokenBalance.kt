package io.horizontalsystems.erc20kit.models

import android.arch.persistence.room.Entity
import java.math.BigInteger

@Entity(primaryKeys = ["primaryKey"])
class TokenBalance(val value: BigInteger, val primaryKey: String = "primaryKey")
