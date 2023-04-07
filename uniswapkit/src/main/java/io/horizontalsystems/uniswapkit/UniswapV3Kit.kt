package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.Quoter.QuoteExactInputSingleMethod
import io.horizontalsystems.uniswapkit.v3.Quoter.QuoteExactOutputSingleMethod
import io.horizontalsystems.uniswapkit.v3.SwapRouter.ExactInputSingleMethod
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class UniswapV3Kit(private val ethereumKit: EthereumKit) {
    private val quoterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0xb27308f9F90D607463bb33eA1BeBb41C27CE5AB6"
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    private val swapRouterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0xE592427A0AEce92De3Edee1F18E0157C05861564"
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    fun bestTradeExactIn(
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger
    ): Single<BigInteger> {
        val fee = BigInteger.valueOf(100)
        val sqrtPriceLimitX96 = BigInteger.ZERO

        return ethereumKit.call(
            contractAddress = Address(quoterAddress),
            data = QuoteExactInputSingleMethod(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                fee = fee,
                amountIn = amountIn,
                sqrtPriceLimitX96 = sqrtPriceLimitX96
            ).encodedABI(),
        ).map {
            it.sliceArray(IntRange(0, 31)).toBigInteger()
        }
    }

    fun bestTradeExactOut(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ): Single<BigInteger> {
        val fee = BigInteger.valueOf(100)
        val sqrtPriceLimitX96 = BigInteger.ZERO

        return ethereumKit.call(
            contractAddress = Address(quoterAddress),
            data = QuoteExactOutputSingleMethod(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                fee = fee,
                amountOut = amountOut,
                sqrtPriceLimitX96 = sqrtPriceLimitX96
            ).encodedABI(),
        ).map {
            it.sliceArray(IntRange(0, 31)).toBigInteger()
        }
    }

    fun transactionData(
        tradeType: TradeType,
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigDecimal,
        amountOut: BigDecimal,
        tradeOptions: TradeOptions
    ): TransactionData {
        val recipient = tradeOptions.recipient ?: ethereumKit.receiveAddress
        val deadline = (Date().time / 1000 + tradeOptions.ttl).toBigInteger()

        return when (tradeType) {
            TradeType.ExactIn -> {
                transactionDataExactIn(
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    recipient = recipient,
                    deadline = deadline,
                    amountIn = amountIn,
                    amountOutMinimum = amountOut,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
            TradeType.ExactOut -> TODO()
        }
    }

    private fun transactionDataExactIn(
        tokenIn: Address,
        tokenOut: Address,
        recipient: Address,
        deadline: BigInteger,
        amountIn: BigDecimal,
        amountOutMinimum: BigDecimal,
        sqrtPriceLimitX96: BigInteger,
    ): TransactionData {
        val ethValue = BigInteger.ZERO

        return TransactionData(
            to = Address(swapRouterAddress),
            value = ethValue,
            input = ExactInputSingleMethod().encodedABI()
        )
    }

    companion object {
        fun getInstance(ethereumKit: EthereumKit): UniswapV3Kit {
            return UniswapV3Kit(ethereumKit)
        }

//        fun addDecorators(ethereumKit: EthereumKit) {
//            ethereumKit.addMethodDecorator(SwapMethodDecorator(SwapContractMethodFactories))
//            ethereumKit.addTransactionDecorator(SwapTransactionDecorator())
//        }

    }

}