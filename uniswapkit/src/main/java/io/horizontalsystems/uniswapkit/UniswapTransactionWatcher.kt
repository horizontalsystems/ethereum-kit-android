package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.ITransactionWatcher
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration

class UniswapTransactionWatcher(
        private val address: Address
) : ITransactionWatcher {

    override fun needInternalTransactions(fullTransaction: FullTransaction): Boolean {
        val decoration = fullTransaction.mainDecoration

        // need internal transaction to get actual amountOut for the case when swapped tokenOut is ETH and recipient is different address
        return fullTransaction.internalTransactions.isEmpty() &&
                decoration is SwapMethodDecoration &&
                decoration.trade is SwapMethodDecoration.Trade.ExactIn &&
                decoration.tokenOut == SwapMethodDecoration.Token.EvmCoin &&
                fullTransaction.transaction.from == address &&
                decoration.to != address

    }

}
