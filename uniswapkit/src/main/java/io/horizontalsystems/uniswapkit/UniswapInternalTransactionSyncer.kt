package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class UniswapInternalTransactionSyncer(private val ethereumKit: EthereumKit) : CoroutineScope {

    private val disposables = CompositeDisposable()
    private var syncingTransactions = mutableMapOf<String, Int>()
    private val maxRetryCount = 3
    private val delayTime = 10 * 1000L // seconds

    private val job = Job()

    init {
        ethereumKit.allTransactionsFlowable
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
            if (decoration is SwapMethodDecoration
                    && decoration.tokenOut is SwapMethodDecoration.Token.EvmCoin
                    && fullTransaction.transaction.from == ethereumKit.receiveAddress
                    && decoration.to != ethereumKit.receiveAddress) {

                val transaction = fullTransaction.transaction
                val count = syncingTransactions[transaction.hash.toHexString()] ?: 0

                if (count < maxRetryCount) {
                    delay(delayTime)
                    syncingTransactions[transaction.hash.toHexString()] = count + 1
                    ethereumKit.syncInternalTransactions(transaction)
                }
            }
        }
    }
}
