package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.models.Transaction

interface Erc20ContractMethodWithTransactions {
    fun getErc20Transactions(ethTx: Transaction): List<io.horizontalsystems.erc20kit.models.Transaction>
}
