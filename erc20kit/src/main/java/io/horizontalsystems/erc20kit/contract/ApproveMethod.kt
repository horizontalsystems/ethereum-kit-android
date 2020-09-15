package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class ApproveMethod(val spender: Address, val value: BigInteger) : Erc20Method {
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

    override fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, listOf(spender, value))
    }

    companion object {
        val methodId = ContractMethodHelper.getMethodId("approve(address,uint256)")
    }
}
