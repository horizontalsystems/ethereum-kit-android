package io.horizontalsystems.oneinchkit

import io.horizontalsystems.ethereumkit.core.ITransactionWatcher
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration

class OneInchTransactionWatcher(
        private val address: Address
) : ITransactionWatcher {

    override fun needInternalTransactions(fullTransaction: FullTransaction): Boolean {
        val decoration = fullTransaction.mainDecoration

        // need internal transaction to get actual toAmount for the case when swapped toToken is ETH and recipient is different address
        return fullTransaction.internalTransactions.isEmpty() &&
                decoration is OneInchSwapMethodDecoration &&
                decoration.toToken == OneInchMethodDecoration.Token.EvmCoin &&
                fullTransaction.transaction.from == address &&
                decoration.recipient != address

    }

}
