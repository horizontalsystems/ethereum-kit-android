package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactory
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.models.Transaction as EthereumTransaction

class Erc20Contract {
    private val erc20MethodFactories = mutableMapOf<Int, ContractMethodFactory>()

    init {
        registerErc20MethodFactory(ApproveMethodFactory)
    }

    private fun registerErc20MethodFactory(factory: ContractMethodFactory) {
        erc20MethodFactories[factory.methodId.toInt()] = factory
    }

    fun getErc20TransactionsFromEthTransaction(ethTx: EthereumTransaction): List<Transaction> {
        val erc20Method = parseMethod(ethTx.input)

        return (erc20Method as? Erc20ContractMethodWithTransactions)?.getErc20Transactions(ethTx) ?: listOf()
    }

    private fun parseMethod(input: ByteArray): ContractMethod? {
        val methodId = input.copyOfRange(0, 4)

        val erc20MethodFactory = erc20MethodFactories[methodId.toInt()]

        return erc20MethodFactory?.createMethod(input.copyOfRange(4, input.size))
    }
}
