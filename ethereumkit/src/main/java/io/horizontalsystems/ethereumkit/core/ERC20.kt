package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit

class ERC20(var contractAddress: String, var listener: EthereumKit.Listener) {

    var balance: String? = null
}
