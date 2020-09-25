package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.hexStringToLongOrNull
import io.horizontalsystems.ethereumkit.core.toHexString

sealed class DefaultBlockParameter {
    class BlockNumber(val value: Long) : DefaultBlockParameter()
    object Earliest : DefaultBlockParameter()
    object Latest : DefaultBlockParameter()
    object Pending : DefaultBlockParameter()

    val raw: String
        get() = when (this) {
            is BlockNumber -> {
                this.value.toHexString()
            }
            Earliest -> {
                EARLIEST_RAW
            }
            Latest -> {
                LATEST_RAW
            }
            Pending -> {
                PENDING_RAW
            }
        }

    companion object {
        private const val EARLIEST_RAW = "earliest"
        private const val LATEST_RAW = "latest"
        private const val PENDING_RAW = "pending"

        fun fromRaw(raw: String): DefaultBlockParameter? {
            return when (raw) {
                EARLIEST_RAW -> Earliest
                LATEST_RAW -> Latest
                PENDING_RAW -> Pending
                else -> {
                    raw.hexStringToLongOrNull()?.let { BlockNumber(it) }
                }
            }
        }
    }
}
