package io.horizontalsystems.uniswapkit

import android.os.Handler
import android.os.Looper
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.reactivex.disposables.CompositeDisposable

class UniswapInternalTransactionSyncer(private val ethereumKit: EthereumKit) {

    private val disposables = CompositeDisposable()
    private var syncingTransactions = mutableMapOf<ByteArray, Int>()
    private val maxRetryCount = 3
    private val delayTime = 10 * 1000L // seconds

    init {
        ethereumKit.allTransactionsFlowable
                .subscribe { transactions ->
                    transactions.forEach { fullTransaction ->
                        handle(fullTransaction)
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    private fun handle(fullTransaction: FullTransaction) {
        if (fullTransaction.internalTransactions.isEmpty()) {

            val decoration = fullTransaction.mainDecoration
            if (decoration is TransactionDecoration.Swap
                    && decoration.tokenOut is TransactionDecoration.Swap.Token.EvmCoin
                    && fullTransaction.transaction.from == ethereumKit.receiveAddress
                    && decoration.to != ethereumKit.receiveAddress) {

                val transaction = fullTransaction.transaction
                val count = syncingTransactions[transaction.hash] ?: 0

                if (count < maxRetryCount){
                    Handler(Looper.getMainLooper()).postDelayed({
                        syncingTransactions[transaction.hash] = count + 1
                        ethereumKit.syncInternalTransactions(transaction)
                    }, delayTime)
                }
            }
        }
    }
}
