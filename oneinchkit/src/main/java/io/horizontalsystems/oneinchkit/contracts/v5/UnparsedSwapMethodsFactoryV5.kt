package io.horizontalsystems.oneinchkit.contracts.v5

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodsFactory
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray

class UnparsedSwapMethodsFactoryV5 : ContractMethodsFactory {

    override val methodIds: List<ByteArray> = listOf(
        "0x84bd6d29".hexStringToByteArray(), //clipperSwap
        "0x093d4fa5".hexStringToByteArray(), //clipperSwapTo
        "0xc805a666".hexStringToByteArray(), //clipperSwapToWithPermit
        "0x62e238bb".hexStringToByteArray(), //fillOrder
        "0x3eca9c0a".hexStringToByteArray(), //fillOrderRFQ
        "0x9570eeee".hexStringToByteArray(), //fillOrderRFQCompact
        "0x5a099843".hexStringToByteArray(), //fillOrderRFQTo
        "0x70ccbd31".hexStringToByteArray(), //fillOrderRFQToWithPermit
        "0xe5d7bde6".hexStringToByteArray(), //fillOrderTo
        "0xd365c695".hexStringToByteArray(), //fillOrderToWithPermit
    )

    override fun createMethod(inputArguments: ByteArray): ContractMethod {
        return UnparsedSwapMethodV5()
    }
}
