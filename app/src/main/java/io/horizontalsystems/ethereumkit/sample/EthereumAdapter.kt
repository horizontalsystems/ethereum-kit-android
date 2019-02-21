package io.horizontalsystems.ethereumkit.sample

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.subjects.PublishSubject

class EthereumAdapter : EthereumKit.Listener {
    val transactionSubject = PublishSubject.create<Unit>()
    val balanceSubject = PublishSubject.create<Unit>()
    val lastBlockHeightSubject = PublishSubject.create<Unit>()
    val kitStateUpdateSubject = PublishSubject.create<Unit>()

    override fun onTransactionsUpdate(ethereumTransactions: List<EthereumTransaction>) {
        transactionSubject.onNext(Unit)
    }

    override fun onBalanceUpdate() {
        balanceSubject.onNext(Unit)
    }

    override fun onLastBlockHeightUpdate() {
        lastBlockHeightSubject.onNext(Unit)
    }

    override fun onSyncStateUpdate() {
        kitStateUpdateSubject.onNext(Unit)
    }

}
