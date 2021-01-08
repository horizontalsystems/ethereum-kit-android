package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Erc20LogEvent
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.ethereumkit.core.removeLeadingZeros
import io.horizontalsystems.ethereumkit.core.stripHexPrefix
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionLog
import java.math.BigInteger

fun TransactionLog.getErc20Event(address: Address): Erc20LogEvent? {
    return try {
        val signature = topics.getOrNull(0)?.hexStringToByteArrayOrNull()
        val firstParam = topics.getOrNull(1)?.let { Address(it.stripHexPrefix().removeLeadingZeros()) }
        val secondParam = topics.getOrNull(2)?.let { Address(it.stripHexPrefix().removeLeadingZeros()) }

        when {
            signature.contentEquals(Erc20LogEvent.Transfer.signature) && firstParam != null && secondParam != null && (firstParam == address || secondParam == address) -> {
                Erc20LogEvent.Transfer(firstParam, secondParam, BigInteger(data))
            }
            signature.contentEquals(Erc20LogEvent.Approve.signature) && firstParam == address && secondParam != null -> {
                Erc20LogEvent.Approve(firstParam, secondParam, BigInteger(data))
            }
            else -> null
        }
    } catch (error: Throwable) {
        error.printStackTrace()
        null
    }
}
