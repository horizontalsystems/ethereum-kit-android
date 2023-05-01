package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactory
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class ExactInputSingleMethod(
    val tokenIn: Address,
    val tokenOut: Address,
    val fee: BigInteger,
    val recipient: Address,
    val amountIn: BigInteger,
    val amountOutMinimum: BigInteger,
    val sqrtPriceLimitX96: BigInteger,
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(
        tokenIn,
        tokenOut,
        fee,
        recipient,
        amountIn,
        amountOutMinimum,
        sqrtPriceLimitX96
    )

    companion object {
        private const val methodSignature =
            "exactInputSingle((address,address,uint24,address,uint256,uint256,uint160))"
    }

    class Factory : ContractMethodFactory {
        override val methodId = ContractMethodHelper.getMethodId(methodSignature)

        override fun createMethod(inputArguments: ByteArray): ContractMethod {
            val parsedArguments = ContractMethodHelper.decodeABI(
                inputArguments, listOf(
                    Address::class,
                    Address::class,
                    BigInteger::class,
                    Address::class,
                    BigInteger::class,
                    BigInteger::class,
                    BigInteger::class,
                )
            )

            return ExactInputSingleMethod(
                tokenIn = parsedArguments[0] as Address,
                tokenOut = parsedArguments[1] as Address,
                fee = parsedArguments[2] as BigInteger,
                recipient = parsedArguments[3] as Address,
                amountIn = parsedArguments[4] as BigInteger,
                amountOutMinimum = parsedArguments[5] as BigInteger,
                sqrtPriceLimitX96 = parsedArguments[6] as BigInteger,
            )
        }
    }
}
