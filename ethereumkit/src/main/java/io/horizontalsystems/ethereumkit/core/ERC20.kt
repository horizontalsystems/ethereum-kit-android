package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.KitState

class ERC20(var listener: EthereumKit.ListenerERC20) {

    var balance: Double = 0.0

    var kitState: KitState = KitState.NotSynced
        set(value) {
            listener.onKitStateUpdate(value)
            field = value
        }
}
