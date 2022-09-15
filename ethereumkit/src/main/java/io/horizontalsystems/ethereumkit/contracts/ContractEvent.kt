package io.horizontalsystems.ethereumkit.contracts

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils

class ContractEvent(
    private val name: String,
    private val arguments: List<Argument>
) {

    val signature: ByteArray by lazy {
        val argumentTypes = arguments.joinToString(separator = ",") { it.type }
        val eventSignature = "$name($argumentTypes)"

        CryptoUtils.sha3(eventSignature.toByteArray())
    }

    enum class Argument(val type: String) {
        Uint256("uint256"),
        Uint256Array("uint256[]"),
        Address("address")
    }

}
