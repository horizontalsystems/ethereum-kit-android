package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigInteger

class KitState {
    var syncState: SyncState = SyncState.NotSynced(SyncError.NotStarted())
        set(value) {
            if (field != value) {
                field = value
                syncStateSubject.tryEmit(value)
            }
        }

    var balance: BigInteger? = null
        set(value) {
            if (value != null && field != value) {
                field = value
                balanceSubject.tryEmit(value)
            }
        }

    val syncStateSubject = MutableSharedFlow<SyncState>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val balanceSubject = MutableSharedFlow<BigInteger>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}
