package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.models.Transaction as EthereumTransaction

class Erc20Contract {
    private val erc20MethodFactories = mutableMapOf<Int, Erc20MethodFactory>()

    init {
        registerErc20MethodFactory(ApproveMethodFactory)
    }

    private fun registerErc20MethodFactory(factory: Erc20MethodFactory) {
        erc20MethodFactories[factory.methodId.toInt()] = factory
    }

    fun getErc20TransactionsFromEthTransaction(ethTx: EthereumTransaction): List<Transaction> {
        return try {
            parseMethod(ethTx.input).getErc20Transactions(ethTx)
        } catch (e: IllegalStateException) {
            listOf()
        }
    }

    private fun parseMethod(input: ByteArray): Erc20Method {
        val methodId = input.copyOfRange(0, 4)

        val erc20MethodFactory = erc20MethodFactories[methodId.toInt()] ?: throw IllegalStateException("Undefined method: ${methodId.toRawHexString()}")

        return erc20MethodFactory.createMethod(input.copyOfRange(4, input.size))
    }
}
