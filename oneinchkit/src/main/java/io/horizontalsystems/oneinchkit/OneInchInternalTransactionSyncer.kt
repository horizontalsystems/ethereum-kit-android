package io.horizontalsystems.oneinchkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class OneInchInternalTransactionSyncer(
        private val evmKit: EthereumKit
) : CoroutineScope {

    private val disposables = CompositeDisposable()
    private var syncingTransactions = mutableMapOf<String, Int>()
    private val maxRetryCount = 3
    private val delayTime = 10 * 1000L // seconds

    private val job = Job()

    init {
        evmKit.allTransactionsFlowable
                .subscribe { transactions ->
                    launch {
                        transactions.forEach { fullTransaction ->
                            handle(fullTransaction)
                        }
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    override val coroutineContext: CoroutineContext = job + Dispatchers.IO

    private suspend fun handle(fullTransaction: FullTransaction) {
        if (fullTransaction.internalTransactions.isEmpty()) {

            val decoration = fullTransaction.mainDecoration
            if (decoration is OneInchSwapMethodDecoration
                    && decoration.toToken is OneInchMethodDecoration.Token.EvmCoin
                    && fullTransaction.transaction.from == evmKit.receiveAddress
                    && decoration.recipient != evmKit.receiveAddress) {

                val transaction = fullTransaction.transaction
                val count = syncingTransactions[transaction.hash.toHexString()] ?: 0

                if (count < maxRetryCount) {
                    delay(delayTime)
                    syncingTransactions[transaction.hash.toHexString()] = count + 1
                    evmKit.syncInternalTransactions(transaction)
                }
            }
        }
    }

}
