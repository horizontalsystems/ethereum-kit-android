package io.horizontalsystems.ethereumkit.sample

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.subjects.PublishSubject

class EthereumAdapter : EthereumKit.Listener {
    val transactionSubject = PublishSubject.create<Int>()
    val balanceSubject = PublishSubject.create<Double>()
    val lastBlockHeightSubject = PublishSubject.create<Int>()
    val kitStateUpdateSubject = PublishSubject.create<EthereumKit.KitState>()

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        transactionSubject.onNext(0)
    }

    override fun onBalanceUpdate(balance: Double) {
        balanceSubject.onNext(balance)
    }

    override fun onLastBlockHeightUpdate(height: Int) {
        lastBlockHeightSubject.onNext(height)
    }

    override fun onKitStateUpdate(state: EthereumKit.KitState) {
        kitStateUpdateSubject.onNext(state)
    }
}
