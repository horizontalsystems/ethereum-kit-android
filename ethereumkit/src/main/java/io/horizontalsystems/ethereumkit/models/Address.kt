package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.utils.EIP55

class Address(rawBytes: ByteArray) {

    // 32-byte inputs are left-padded words (e.g. event topics); the address is the last 20 bytes.
    val raw: ByteArray = if (rawBytes.size == 32) rawBytes.copyOfRange(12, rawBytes.size) else rawBytes

    init {
        AddressValidator.validate(hex)
    }

    constructor(hex: String) : this(hex.hexStringToByteArray())

    val hex: String
        get() = raw.toHexString()

    // A benign race may compute this twice; both results are identical.
    private var eip55Cache: String? = null
    val eip55: String
        get() = eip55Cache ?: EIP55.format(hex).also { eip55Cache = it }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true

        return if (other is Address)
            raw.contentEquals(other.raw)
        else false
    }

    override fun hashCode(): Int {
        return raw.contentHashCode()
    }

    override fun toString(): String {
        return hex
    }

}
