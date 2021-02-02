package io.horizontalsystems.ethereumkit.models

import androidx.room.Embedded
import androidx.room.Relation
import java.math.BigInteger

class FullTransaction(
        @Embedded
        val transaction: Transaction,
        @Relation(
                entity = TransactionReceipt::class,
                parentColumn = "hash",
                entityColumn = "transactionHash"
        )
        val receiptWithLogs: TransactionReceiptWithLogs? = null,
        @Relation(
                entity = InternalTransaction::class,
                parentColumn = "hash",
                entityColumn = "hash"
        )
        val internalTransactions: List<InternalTransaction> = listOf(),
        @Relation(
                entity = DroppedTransaction::class,
                parentColumn = "hash",
                entityColumn = "hash"
        )
        val droppedTransaction: DroppedTransaction? = null
) {
    fun isFailed(): Boolean {
        val receipt = receiptWithLogs?.receipt
        return when {
            droppedTransaction != null -> true
            receipt == null -> false
            receipt.status == null -> transaction.gasLimit == receipt.cumulativeGasUsed
            else -> receipt.status == 0
        }
    }

    fun hasEtherTransfer(address: Address): Boolean =
            transaction.from == address && transaction.value > BigInteger.ZERO ||
                    transaction.to == address ||
                    internalTransactions.any { it.to == address }

}
