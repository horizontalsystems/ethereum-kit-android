package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.FeePriority
import io.reactivex.Single

class EthereumAdapter(ethereumKit: EthereumKit): BaseAdapter(ethereumKit, 18) {

    init {
        ethereumKit.listener = this
    }

    override val syncState: EthereumKit.SyncState
        get() = ethereumKit.syncState

    override val balanceString: String?
        get() = ethereumKit.balance

    override fun sendSingle(address: String, amount: String, feePriority: FeePriority): Single<Unit> {
        return ethereumKit.send(address, amount, feePriority).map { Unit }
    }

    override fun transactionsObservable(hashFrom: String?, limit: Int?): Single<List<EthereumTransaction>> {
        return ethereumKit.transactions(hashFrom, limit)
    }

}
