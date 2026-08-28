package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import java.math.BigDecimal

class EthereumAdapter(
        private val ethereumKit: EthereumKit,
        private val signer: Signer
) : EthereumBaseAdapter(ethereumKit) {

    private val decimal = 18

    override suspend fun send(
            address: Address,
            amount: BigDecimal,
            gasPrice: GasPrice,
            gasLimit: Long
    ): FullTransaction {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        val transactionData = ethereumKit.transferTransactionData(address, amountBigInt)
        val rawTransaction = ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit)
        val signature = signer.signature(rawTransaction)
        return ethereumKit.send(rawTransaction, signature)
    }

}
