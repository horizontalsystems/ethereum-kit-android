package io.horizontalsystems.ethereumkit.api.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigInteger
import java.util.*

@Entity
class AccountState(
        val balance: BigInteger,
        val nonce: Long,
        @PrimaryKey val id: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (other !is AccountState)
            return false

        return balance == other.balance && nonce == other.nonce
    }

    override fun hashCode(): Int {
        return Objects.hash(balance, nonce)
    }
}
