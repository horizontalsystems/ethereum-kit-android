package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.ethereumkit.models.EtherscanTransaction as EthereumTransaction

class Erc20Contract {

    init {
        ContractMethodFactories.registerMethodFactory(ApproveMethodFactory)
    }

    fun getErc20TransactionsFromEthTransaction(ethTx: EthereumTransaction): List<Transaction> {
        val contractMethod = ContractMethodFactories.createMethodFromInput(ethTx.input)

        return listOf()//(contractMethod as? Erc20ContractMethodWithTransactions)?.getErc20Transactions(ethTx) ?: listOf()
    }

}
