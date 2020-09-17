package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument.*
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionWithInternal
import io.horizontalsystems.uniswapkit.models.*
import io.horizontalsystems.uniswapkit.models.Token.Erc20
import io.horizontalsystems.uniswapkit.models.Token.Ether
import io.reactivex.Single
import java.math.BigInteger
import java.util.*
import java.util.logging.Logger

class TradeManager(
        private val ethereumKit: EthereumKit
) {
    private val address: Address = ethereumKit.receiveAddress
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    fun pair(tokenA: Token, tokenB: Token): Single<Pair> {
        val method = ContractMethod("getReserves")

        val (token0, token1) = if (tokenA.sortsBefore(tokenB)) Pair(tokenA, tokenB) else Pair(tokenB, tokenA)

        val pairAddress = Pair.address(token0, token1)

        logger.info("pairAddress: ${pairAddress.hex}")

        return ethereumKit.call(pairAddress, method.encodedABI())
                .map { data ->
                    logger.info("getReserves data: ${data.toHexString()}")

                    var rawReserve0: BigInteger = BigInteger.ZERO
                    var rawReserve1: BigInteger = BigInteger.ZERO

                    if (data.size == 3 * 32) {
                        rawReserve0 = BigInteger(data.copyOfRange(0, 32))
                        rawReserve1 = BigInteger(data.copyOfRange(32, 64))
                    }

                    val reserve0 = TokenAmount(token0, rawReserve0)
                    val reserve1 = TokenAmount(token1, rawReserve1)

                    logger.info("getReserves reserve0: $reserve0, reserve1: $reserve1")

                    Pair(reserve0, reserve1)
                }
    }

    fun estimateSwap(tradeData: TradeData, gasPrice: Long): Single<Long> {
        val swapData = buildSwapData(tradeData)

        return ethereumKit.estimateGas(
                to = routerAddress,
                value = if (swapData.amount == BigInteger.ZERO) null else swapData.amount,
                gasPrice = gasPrice,
                data = swapData.input
        )
    }

    fun swap(tradeData: TradeData, gasPrice: Long, gasLimit: Long): Single<TransactionWithInternal> {
        val swapData = buildSwapData(tradeData)

        return ethereumKit.send(routerAddress, swapData.amount, swapData.input, gasPrice, gasLimit)
    }

    private class SwapData(val amount: BigInteger, val input: ByteArray)

    private fun buildSwapData(tradeData: TradeData): SwapData {
        val methodName: String
        val arguments: List<Argument>
        val amount: BigInteger

        val trade = tradeData.trade

        val tokenIn = trade.tokenAmountIn.token
        val tokenOut = trade.tokenAmountOut.token

        val path = AddressesArgument(trade.route.path.map { it.address })
        val to = AddressArgument(tradeData.options.recipient ?: address)
        val deadline = Uint256Argument((Date().time / 1000 + tradeData.options.ttl).toBigInteger())

        when (trade.type) {
            TradeType.ExactIn -> {
                val amountIn = trade.tokenAmountIn.rawAmount
                val amountOutMin = tradeData.tokenAmountOutMin.rawAmount

                amount = amountIn

                if (tokenIn is Ether && tokenOut is Erc20) {
                    methodName = if (tradeData.options.feeOnTransfer) "swapExactETHForTokensSupportingFeeOnTransferTokens" else "swapExactETHForTokens"
                    arguments = listOf(Uint256Argument(amountOutMin), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Ether) {
                    methodName = if (tradeData.options.feeOnTransfer) "swapExactTokensForETHSupportingFeeOnTransferTokens" else "swapExactTokensForETH"
                    arguments = listOf(Uint256Argument(amountIn), Uint256Argument(amountOutMin), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Erc20) {
                    methodName = if (tradeData.options.feeOnTransfer) "swapExactTokensForTokensSupportingFeeOnTransferTokens" else "swapExactTokensForTokens"
                    arguments = listOf(Uint256Argument(amountIn), Uint256Argument(amountOutMin), path, to, deadline)
                } else {
                    throw Exception("Invalid tokenIn/Out for swap!")
                }
            }
            TradeType.ExactOut -> {
                val amountInMax = tradeData.tokenAmountInMax.rawAmount
                val amountOut = trade.tokenAmountOut.rawAmount

                amount = amountInMax

                if (tokenIn is Ether && tokenOut is Erc20) {
                    methodName = "swapETHForExactTokens"
                    arguments = listOf(Uint256Argument(amountOut), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Ether) {
                    methodName = "swapTokensForExactETH"
                    arguments = listOf(Uint256Argument(amountOut), Uint256Argument(amountInMax), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Erc20) {
                    methodName = "swapTokensForExactTokens"
                    arguments = listOf(Uint256Argument(amountOut), Uint256Argument(amountInMax), path, to, deadline)
                } else {
                    throw Exception("Invalid tokenIn/Out for swap!")
                }
            }
        }

        val method = ContractMethod(methodName, arguments)

        return if (tokenIn.isEther) {
            SwapData(amount, method.encodedABI())
        } else {
            SwapData(BigInteger.ZERO, method.encodedABI())
        }
    }

    companion object {
        val routerAddress = Address("0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D")

        fun tradeExactIn(pairs: List<Pair>, tokenAmountIn: TokenAmount, tokenOut: Token, maxHops: Int = 3, currentPairs: List<Pair> = listOf(), originalTokenAmountIn: TokenAmount? = null): List<Trade> {
            //todo validations

            val trades = mutableListOf<Trade>()
            val originalTokenAmountIn = originalTokenAmountIn ?: tokenAmountIn

            for ((index, pair) in pairs.withIndex()) {

                val tokenAmountOut = try {
                    pair.tokenAmountOut(tokenAmountIn)
                } catch (error: Throwable) {
                    continue
                }

                if (tokenAmountOut.token == tokenOut) {
                    val trade = Trade(
                            TradeType.ExactIn,
                            Route(currentPairs + listOf(pair), originalTokenAmountIn.token, tokenOut),
                            originalTokenAmountIn,
                            tokenAmountOut
                    )
                    trades.add(trade)
                } else if (maxHops > 1 && pairs.size > 1) {
                    val pairsExcludingThisPair = pairs.toMutableList().apply { removeAt(index) }
                    val tradesRecursion = tradeExactIn(
                            pairsExcludingThisPair,
                            tokenAmountOut,
                            tokenOut,
                            maxHops - 1,
                            currentPairs + listOf(pair),
                            originalTokenAmountIn
                    )
                    trades.addAll(tradesRecursion)
                }
            }
            return trades
        }

        fun tradeExactOut(pairs: List<Pair>, tokenIn: Token, tokenAmountOut: TokenAmount, maxHops: Int = 3, currentPairs: List<Pair> = listOf(), originalTokenAmountOut: TokenAmount? = null): List<Trade> {
            //todo validations

            val trades = mutableListOf<Trade>()
            val originalTokenAmountOut = originalTokenAmountOut ?: tokenAmountOut

            for ((index, pair) in pairs.withIndex()) {

                val tokenAmountIn = try {
                    pair.tokenAmountIn(tokenAmountOut)
                } catch (error: Throwable) {
                    continue
                }

                if (tokenAmountIn.token == tokenIn) {
                    val trade = Trade(
                            TradeType.ExactOut,
                            Route(listOf(pair) + currentPairs, tokenIn, originalTokenAmountOut.token),
                            tokenAmountIn,
                            originalTokenAmountOut
                    )
                    trades.add(trade)
                } else if (maxHops > 1 && pairs.size > 1) {
                    val pairsExcludingThisPair = pairs.toMutableList().apply { removeAt(index) }
                    val tradesRecursion = tradeExactOut(
                            pairsExcludingThisPair,
                            tokenIn,
                            tokenAmountIn,
                            maxHops - 1,
                            currentPairs + listOf(pair),
                            originalTokenAmountOut
                    )
                    trades.addAll(tradesRecursion)
                }
            }
            return trades
        }

    }

}
