package io.horizontalsystems.ethereumkit.sample

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class EthereumAdapter : EthereumKit.Listener {
    val transactionSubject = PublishSubject.create<Int>()
    val balanceSubject = PublishSubject.create<BigDecimal>()
    val lastBlockHeightSubject = PublishSubject.create<Int>()
    val kitStateUpdateSubject = PublishSubject.create<EthereumKit.SyncState>()

    override fun onTransactionsUpdate(ethereumTransactions: List<EthereumTransaction>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBalanceUpdate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLastBlockHeightUpdate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSyncStateUpdate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
//        transactionSubject.onNext(0)
//    }
//
//    override fun onBalanceUpdate(balance: BigDecimal) {
//        balanceSubject.onNext(balance)
//    }
//
//    override fun onLastBlockHeightUpdate(height: Int) {
//        lastBlockHeightSubject.onNext(height)
//    }
//
//    override fun onKitStateUpdate(state: EthereumKit.SyncState) {
//        kitStateUpdateSubject.onNext(state)
//    }
}
