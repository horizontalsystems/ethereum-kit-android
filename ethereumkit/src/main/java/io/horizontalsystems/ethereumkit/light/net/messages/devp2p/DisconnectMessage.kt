package io.horizontalsystems.ethereumkit.light.net.messages.devp2p

import io.horizontalsystems.ethereumkit.light.net.messages.IMessage
import io.horizontalsystems.ethereumkit.light.rlp.RLP
import io.horizontalsystems.ethereumkit.light.rlp.RLPList

class DisconnectMessage : IMessage {

    private var reason: ReasonCode = ReasonCode.UNKNOWN
    override var code: Int = DisconnectMessage.code

    companion object {
        const val code = 0x01
    }

    override fun encoded(): ByteArray {
        val encodedReason = RLP.encodeByte(this.reason.asByte())
        return RLP.encodeList(encodedReason)
    }

    constructor(reason: ReasonCode) {
        this.reason = reason
    }

    constructor(payload: ByteArray) {
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
