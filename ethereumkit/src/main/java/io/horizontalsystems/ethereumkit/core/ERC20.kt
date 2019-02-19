package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.SyncState
import java.math.BigDecimal

class ERC20(var contractAddress: String, var decimal: Int, var listener: EthereumKit.Listener) {

    var balance: BigDecimal? = null
    var syncState: SyncState? = null
}
