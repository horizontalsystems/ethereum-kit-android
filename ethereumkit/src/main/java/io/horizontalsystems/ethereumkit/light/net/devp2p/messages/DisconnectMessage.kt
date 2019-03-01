package io.horizontalsystems.ethereumkit.light.net.devp2p.messages

import io.horizontalsystems.ethereumkit.light.net.IP2PMessage
import io.horizontalsystems.ethereumkit.light.rlp.RLP
import io.horizontalsystems.ethereumkit.light.rlp.RLPList

class DisconnectMessage : IP2PMessage {

    private var reason: ReasonCode = ReasonCode.UNKNOWN
    override var code: Int = Companion.code

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
