package io.horizontalsystems.erc20kit.models

enum class TransactionType(val value: String) {
    TRANSFER("transfer"),
    APPROVE("approve");

    companion object {
        fun valueOf(value: String?): TransactionType? {
            return values().find { it.value == value }
        }
    }
}
