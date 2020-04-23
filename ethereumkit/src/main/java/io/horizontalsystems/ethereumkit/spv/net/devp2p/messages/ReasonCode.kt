package io.horizontalsystems.ethereumkit.spv.net.devp2p.messages

enum class ReasonCode(val code: Int) {

    REQUESTED(0x00),
    TCP_ERROR(0x01),
    BAD_PROTOCOL(0x02),
    USELESS_PEER(0x03),
    TOO_MANY_PEERS(0x04),
    DUPLICATE_PEER(0x05),
    INCOMPATIBLE_PROTOCOL(0x06),
    NULL_IDENTITY(0x07),
    PEER_QUITING(0x08),
    UNEXPECTED_IDENTITY(0x09),
    LOCAL_IDENTITY(0x0A),
    PING_TIMEOUT(0x0B),
    USER_REASON(0x10),
    UNKNOWN(0xFF);

    fun asByte(): Byte {
        return code.toByte()
    }

    companion object {
        private val intToTypeMap: MutableMap<Int, ReasonCode> = hashMapOf()

        init {
            for (type in values()) {
                intToTypeMap[type.code] = type
            }
        }

        fun fromInt(i: Int): ReasonCode {
            return intToTypeMap[i] ?: return UNKNOWN
        }
    }
}
