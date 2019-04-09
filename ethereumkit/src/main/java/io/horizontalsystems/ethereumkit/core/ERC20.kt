package io.horizontalsystems.ethereumkit.core

import java.math.BigInteger

class ERC20(var contractAddress: ByteArray, var listener: EthereumKit.Listener) {

    var balance: BigInteger? = null
}
