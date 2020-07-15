package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.uniswapkit.ContractMethod.Argument
import io.horizontalsystems.uniswapkit.ContractMethod.Argument.*
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
    private val address: ByteArray = ethereumKit.receiveAddressRaw
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    fun getPair(tokenA: Token, tokenB: Token): Single<Pair> {
        val method = ContractMethod("getReserves")

        val (token0, token1) = if (tokenA.sortsBefore(tokenB)) Pair(tokenA, tokenB) else Pair(tokenB, tokenA)

        val pairAddress = Pair.address(token0, token1)

        logger.info("pairAddress: ${pairAddress.toHexString()}")

        return ethereumKit.call(pairAddress, method.encodedABI())
                .map { data ->
                    logger.info("getReserves data: ${data.toHexString()}")

                    var reserve0: BigInteger = BigInteger.ZERO
                    var reserve1: BigInteger = BigInteger.ZERO

                    if (data.size == 3 * 32) {
                        reserve0 = BigInteger(data.copyOfRange(0, 32))
                        reserve1 = BigInteger(data.copyOfRange(32, 64))
                    }

                    val tokenAmount0 = TokenAmount(token0, reserve0)
                    val tokenAmount1 = TokenAmount(token1, reserve1)

                    logger.info("getReserves reserve0: $tokenAmount0, reserve1: $tokenAmount1")

                    Pair(tokenAmount0, tokenAmount1)
                }
    }

    fun swap(tradeData: TradeData): Single<String> {
        val methodName: String
        val arguments: List<Argument>
        val amount: BigInteger

        val trade = tradeData.trade

        val tokenIn = trade.tokenAmountIn.token
        val tokenOut = trade.tokenAmountOut.token

        val path = Addresses(trade.route.path.map { it.address })
        val to = Address(tradeData.options.recipient ?: address)
        val deadline = Uint256((Date().time / 1000 + tradeData.options.ttl).toBigInteger())

        when (trade.type) {
            TradeType.ExactIn -> {
                val amountIn = trade.tokenAmountIn.amount
                val amountOutMin = tradeData.tokenAmountOutMin.amount

                amount = amountIn

                if (tokenIn is Ether && tokenOut is Erc20) {
                    methodName = if (tradeData.options.feeOnTransfer) "swapExactETHForTokensSupportingFeeOnTransferTokens" else "swapExactETHForTokens"
                    arguments = listOf(Uint256(amountOutMin), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Ether) {
                    methodName = if (tradeData.options.feeOnTransfer) "swapExactTokensForETHSupportingFeeOnTransferTokens" else "swapExactTokensForETH"
                    arguments = listOf(Uint256(amountIn), Uint256(amountOutMin), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Erc20) {
                    methodName = if (tradeData.options.feeOnTransfer) "swapExactTokensForTokensSupportingFeeOnTransferTokens" else "swapExactTokensForTokens"
                    arguments = listOf(Uint256(amountIn), Uint256(amountOutMin), path, to, deadline)
                } else {
                    throw Exception("Invalid tokenIn/Out for swap!")
                }
            }
            TradeType.ExactOut -> {
                val amountInMax = tradeData.tokenAmountInMax.amount
                val amountOut = trade.tokenAmountOut.amount

                amount = amountInMax

                if (tokenIn is Ether && tokenOut is Erc20) {
                    methodName = "swapETHForExactTokens"
                    arguments = listOf(Uint256(amountOut), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Ether) {
                    methodName = "swapTokensForExactETH"
                    arguments = listOf(Uint256(amountOut), Uint256(amountInMax), path, to, deadline)
                } else if (tokenIn is Erc20 && tokenOut is Erc20) {
                    methodName = "swapTokensForExactTokens"
                    arguments = listOf(Uint256(amountOut), Uint256(amountInMax), path, to, deadline)
                } else {
                    throw Exception("Invalid tokenIn/Out for swap!")
                }
            }
        }

        val method = ContractMethod(methodName, arguments)

        return if (tokenIn.isEther) {
            swap(amount, method.encodedABI())
        } else {
            swapWithApprove(tokenIn.address, amount, swap(BigInteger.ZERO, method.encodedABI()))
        }
    }

    private fun swap(value: BigInteger, input: ByteArray): Single<String> {
        return ethereumKit.send(routerAddress, value, input, 50_000_000_000, 500_000)
                .map { txInfo ->
                    logger.info("Swap tx hash: ${txInfo.hash}")
                    txInfo.hash
                }
    }

    private fun swapWithApprove(contractAddress: ByteArray, amount: BigInteger, swapSingle: Single<String>): Single<String> {
        val approveTransactionInput = ContractMethod("approve", listOf(Address(routerAddress), Uint256(amount))).encodedABI()
        return ethereumKit.send(contractAddress, BigInteger.ZERO, approveTransactionInput, 50_000_000_000, 500_000)
                .flatMap { txInfo ->
                    logger.info("Approve tx hash: ${txInfo.hash}")
                    swapSingle
                }
    }

    companion object {
        private val routerAddress = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D".hexStringToByteArray()

        fun bestTradeExactIn(pairs: List<Pair>, tokenAmountIn: TokenAmount, tokenOut: Token, maxHops: Int = 3, currentPairs: List<Pair> = listOf(), originalTokenAmountIn: TokenAmount? = null): List<Trade> {
            //todo validations

            val bestTrades = mutableListOf<Trade>()
            val originalTokenAmountIn = originalTokenAmountIn ?: tokenAmountIn
            val tokenIn = tokenAmountIn.token

            for ((index, pair) in pairs.withIndex()) {
                if (pair.token0 != tokenIn && pair.token1 != tokenIn) {
                    continue
                }
                if (pair.reserve0 == BigInteger.ZERO || pair.reserve1 == BigInteger.ZERO) {
                    continue
                }

                val tokenAmountOut = pair.tokenAmountOut(tokenAmountIn)

                if (tokenAmountOut.token == tokenOut) {
                    val trade = Trade(
                            TradeType.ExactIn,
                            Route(currentPairs + listOf(pair), originalTokenAmountIn.token, tokenOut),
                            originalTokenAmountIn,
                            tokenAmountOut
                    )
                    bestTrades.add(trade)
                } else if (maxHops > 1 && pairs.size > 1) {
                    val pairsExcludingThisPair = pairs.drop(index)
                    val trades = bestTradeExactIn(
                            pairsExcludingThisPair,
                            tokenAmountOut,
                            tokenOut,
                            maxHops - 1,
                            currentPairs + listOf(pair),
                            originalTokenAmountIn
                    )
                    bestTrades.addAll(trades)
                }
            }
            return bestTrades
        }

        fun bestTradeExactOut(pairs: List<Pair>, tokenIn: Token, tokenAmountOut: TokenAmount, maxHops: Int = 3, currentPairs: List<Pair> = listOf(), originalTokenAmountOut: TokenAmount? = null): List<Trade> {
            //todo validations

            val bestTrades = mutableListOf<Trade>()
            val originalTokenAmountOut = originalTokenAmountOut ?: tokenAmountOut
            val tokenOut = tokenAmountOut.token

            for ((index, pair) in pairs.withIndex()) {
                if (pair.token0 != tokenOut && pair.token1 != tokenOut) {
                    continue
                }
                if (pair.reserve0 == BigInteger.ZERO || pair.reserve1 == BigInteger.ZERO) {
                    continue
                }

                val tokenAmountIn = try {
                    pair.tokenAmountIn(tokenAmountOut)
                } catch (ex: Exception) {
                    continue
                }

                if (tokenAmountOut.token == tokenIn) {
                    val trade = Trade(
                            TradeType.ExactOut,
                            Route(listOf(pair) + currentPairs, tokenIn, originalTokenAmountOut.token),
                            tokenAmountIn,
                            tokenAmountOut
                    )
                    bestTrades.add(trade)
                } else if (maxHops > 1 && pairs.size > 1) {
                    val pairsExcludingThisPair = pairs.drop(index)
                    val trades = bestTradeExactOut(
                            pairsExcludingThisPair,
                            tokenIn,
                            tokenAmountIn,
                            maxHops - 1,
                            currentPairs + listOf(pair),
                            originalTokenAmountOut
                    )
                    bestTrades.addAll(trades)
                }
            }
            return bestTrades
        }

    }

}
