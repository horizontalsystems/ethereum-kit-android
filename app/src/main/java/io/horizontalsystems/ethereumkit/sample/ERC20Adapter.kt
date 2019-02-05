package io.horizontalsystems.ethereumkit.sample

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class ERC20Adapter(override val contractAddress: String, override val decimal: Int) : EthereumKit.ListenerERC20 {
    val transactionSubject = PublishSubject.create<Void>()
    val balanceSubject = PublishSubject.create<BigDecimal>()
    val lastBlockHeightSubject = PublishSubject.create<Int>()
    val kitStateUpdateSubject = PublishSubject.create<EthereumKit.KitState>()

    override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        transactionSubject.onComplete()
    }

    override fun onBalanceUpdate(balance: BigDecimal) {
        balanceSubject.onNext(balance)
    }

    override fun onLastBlockHeightUpdate(height: Int) {
        lastBlockHeightSubject.onNext(height)
    }

    override fun onKitStateUpdate(state: EthereumKit.KitState) {
        kitStateUpdateSubject.onNext(state)
    }
}
