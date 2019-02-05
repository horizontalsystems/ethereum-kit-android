package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.KitState
import java.math.BigDecimal

class ERC20(var listener: EthereumKit.ListenerERC20) {

    var balance: BigDecimal = BigDecimal.valueOf(0.0)

    var kitState: KitState = KitState.NotSynced
        set(value) {
            listener.onKitStateUpdate(value)
            field = value
        }
}
