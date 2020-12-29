package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class KitState {
    var syncState: SyncState = SyncState.NotSynced(SyncError.NotStarted())
        set(value) {
            if (field != value) {
                field = value
                syncStateSubject.onNext(value)
            }
        }

    var balance: BigInteger? = null
        set(value) {
            if (value != null && field != value) {
                field = value
                balanceSubject.onNext(value)
            }
        }

    val syncStateSubject = PublishSubject.create<SyncState>()
    val balanceSubject = PublishSubject.create<BigInteger>()
}
