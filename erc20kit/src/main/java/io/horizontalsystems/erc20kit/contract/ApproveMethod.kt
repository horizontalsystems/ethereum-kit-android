package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class ApproveMethod(val spender: Address, val value: BigInteger) : Erc20Method() {

    override val methodSignature = "approve(address,uint256)"
    override fun getArguments() = listOf(spender, value)

    override fun getErc20Transactions(ethTx: io.horizontalsystems.ethereumkit.models.Transaction): List<Transaction> {
        return listOf(
                Transaction(
                        transactionHash = ethTx.hash,
                        interTransactionIndex = 0,
                        transactionIndex = ethTx.transactionIndex,
                        from = ethTx.from,
                        to = spender,
                        value = value,
                        timestamp = ethTx.timestamp,
                        type = Transaction.TransactionType.APPROVE
                ).apply {
                    blockHash = ethTx.blockHash
                    blockNumber = ethTx.blockNumber
                }
        )
    }

}
