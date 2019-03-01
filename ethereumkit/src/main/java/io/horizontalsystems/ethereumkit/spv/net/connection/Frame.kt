package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.core.toHexString

class Frame(var type: Int, var payload: ByteArray) {
    var size: Int = 0

    var totalFrameSize = -1
    var contextId = -1

    constructor(type: Int, payload: ByteArray, totalFrameSize: Int, contextId: Int) : this(type, payload) {
        this.totalFrameSize = totalFrameSize
        this.contextId = contextId
    }

    init {
        this.size = payload.size
    }

    override fun toString(): String {
        return "Frame [type: $type; size: $size; payload: ${payload.toHexString()}; " +
                "totalFrameSize: $totalFrameSize; contextId: $contextId]"
    }
}