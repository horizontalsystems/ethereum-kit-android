package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactory
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

object SwapExactTokensForTokensMethodFactory : ContractMethodFactory {
    override val methodId = ContractMethodHelper.getMethodId(SwapExactTokensForTokensMethod.methodSignature)

    override fun createMethod(inputArguments: ByteArray): ContractMethod {
        val parsedArguments = ContractMethodHelper.decodeABI(inputArguments, listOf(BigInteger::class, BigInteger::class, List::class, Address::class, BigInteger::class))
        val amountIn = parsedArguments[0] as BigInteger
        val amountOutMin = parsedArguments[1] as BigInteger
        val path = parsedArguments[2] as List<Address>
        val to = parsedArguments[3] as Address
        val deadline = parsedArguments[4] as BigInteger
        return SwapExactTokensForTokensMethod(amountIn, amountOutMin, path, to, deadline)
    }

}
