package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.events.ApproveEventDecoration
import io.horizontalsystems.erc20kit.events.TransferEventDecoration
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionLog
import java.math.BigInteger

fun TransactionLog.getErc20Event(): EventDecoration? {
    return try {
        if (topics.size != 3) {
            return null
        }

        val signature = topics[0].hexStringToByteArrayOrNull()

        val firstParam = Address(topics[1])
        val secondParam = Address(topics[2])

        when {
            signature.contentEquals(TransferEventDecoration.signature) ->
                TransferEventDecoration(address, firstParam, secondParam, BigInteger(data.toRawHexString(), 16))
            signature.contentEquals(ApproveEventDecoration.signature) ->
                ApproveEventDecoration(address, firstParam, secondParam, BigInteger(data.toRawHexString(), 16))
            else ->
                null
        }
    } catch (error: Throwable) {
        error.printStackTrace()
        null
    }
}
