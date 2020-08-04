package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class KitState {
    var syncState: Erc20Kit.SyncState = Erc20Kit.SyncState.NotSynced(SyncError.NotStarted())
        set(value) {
            if (field != value) {
                field = value
                syncStateSubject.onNext(value)
            }
        }

    var transactionsSyncState: Erc20Kit.SyncState = Erc20Kit.SyncState.NotSynced(SyncError.NotStarted())
        set(value) {
            if (field != value) {
                field = value
                transactionsSyncStateSubject.onNext(value)
            }
        }

    var balance: BigInteger? = null
        set(value) {
            if (value != null && field != value) {
                field = value
                balanceSubject.onNext(value)
            }
        }

    val syncStateSubject = PublishSubject.create<Erc20Kit.SyncState>()
    val transactionsSyncStateSubject = PublishSubject.create<Erc20Kit.SyncState>()
    val balanceSubject = PublishSubject.create<BigInteger>()
    val transactionsSubject = PublishSubject.create<List<Transaction>>()
}
