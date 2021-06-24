package io.horizontalsystems.uniswapkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapEventDecoration(
        contractAddress: Address,
        val sender: Address,
        val amount0In: BigInteger,
        val amount1In: BigInteger,
        val amount0Out: BigInteger,
        val amount1Out: BigInteger,
        val to: Address
) : ContractEventDecoration(contractAddress) {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        return if (fromAddress == userAddress) {
            listOf("swap")
        } else {
            emptyList()
        }
    }

    companion object {
        val signature = ContractEvent(
                "Swap",
                listOf(
                        ContractEvent.Argument.Address,
                        ContractEvent.Argument.Uint256,
                        ContractEvent.Argument.Uint256,
                        ContractEvent.Argument.Uint256,
                        ContractEvent.Argument.Uint256,
                        ContractEvent.Argument.Address
                )
        ).signature
    }
}
