package io.horizontalsystems.ethereumkit.spv.net.devp2p.messages

import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList

class DisconnectMessage(payload: ByteArray) : IInMessage {
    private var reason: ReasonCode = ReasonCode.UNKNOWN

    init {
        val paramsList = RLP.decode2(payload)[0] as RLPList
        reason = if (paramsList.size > 0) {
            val reasonBytes = paramsList[0].rlpData
            if (reasonBytes == null)
                ReasonCode.UNKNOWN
            else
                ReasonCode.fromInt(reasonBytes[0].toInt())
        } else {
            ReasonCode.UNKNOWN
        }
    }

    override fun toString(): String {
        return "DisconnectMessage [reason: ${reason.name}; code: ${reason.code}]"
    }
}
