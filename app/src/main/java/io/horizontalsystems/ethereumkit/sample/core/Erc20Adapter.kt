package io.horizontalsystems.ethereumkit.sample.core

import android.content.Context
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.sample.modules.main.Erc20Token
import java.math.BigDecimal

class Erc20Adapter(
    context: Context,
    token: Erc20Token,
    private val ethereumKit: EthereumKit,
    private val signer: Signer
) : Erc20BaseAdapter(context, token, ethereumKit) {

    override suspend fun send(address: Address, amount: BigDecimal, gasPrice: GasPrice, gasLimit: Long): FullTransaction {
        val valueBigInteger = amount.movePointRight(decimals).toBigInteger()
        val transactionData = erc20Kit.buildTransferTransactionData(address, valueBigInteger)

        val rawTransaction = ethereumKit.rawTransaction(transactionData, gasPrice, gasLimit)
        val signature = signer.signature(rawTransaction)
        return ethereumKit.send(rawTransaction, signature)
    }

    suspend fun allowance(spenderAddress: Address): BigDecimal {
        return erc20Kit.getAllowanceAsync(spenderAddress).toBigDecimal().movePointLeft(decimals)
    }

}
