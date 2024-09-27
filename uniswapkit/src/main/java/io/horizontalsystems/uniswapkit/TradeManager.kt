package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.contract.GetReservesMethod
import io.horizontalsystems.uniswapkit.contract.SwapETHForExactTokensMethod
import io.horizontalsystems.uniswapkit.contract.SwapExactETHForTokensMethod
import io.horizontalsystems.uniswapkit.contract.SwapExactTokensForETHMethod
import io.horizontalsystems.uniswapkit.contract.SwapExactTokensForTokensMethod
import io.horizontalsystems.uniswapkit.contract.SwapTokensForExactETHMethod
import io.horizontalsystems.uniswapkit.contract.SwapTokensForExactTokensMethod
import io.horizontalsystems.uniswapkit.models.*
import io.horizontalsystems.uniswapkit.models.Token.Erc20
import io.horizontalsystems.uniswapkit.models.Token.Ether
import io.reactivex.Single
import java.math.BigInteger
import java.util.Date
import java.util.logging.Logger

class TradeManager {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    sealed class UnsupportedChainError : Throwable() {
        object NoRouterAddress : UnsupportedChainError()
        object NoFactoryAddress : UnsupportedChainError()
        object NoInitCodeHash : UnsupportedChainError()
    }

    fun pair(rpcSource: RpcSource, chain: Chain, tokenA: Token, tokenB: Token): Single<Pair> {
        val (token0, token1) = if (tokenA.sortsBefore(tokenB)) Pair(tokenA, tokenB) else Pair(tokenB, tokenA)
        val factoryAddressString = getFactoryAddressString(chain)
        val initCodeHashString = getInitCodeHashString(chain)

        val pairAddress = Pair.address(token0, token1, factoryAddressString, initCodeHashString)

        logger.info("pairAddress: ${pairAddress.hex}")

        return EthereumKit.call(rpcSource, pairAddress, GetReservesMethod().encodedABI())
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

    fun transactionData(receiveAddress: Address, chain: Chain, tradeData: TradeData): TransactionData {
        val routerAddress = getRouterAddress(chain)

        return buildSwapData(receiveAddress, tradeData).let {

            TransactionData(routerAddress, it.amount, it.input)
        }
    }

    private class SwapData(val amount: BigInteger, val input: ByteArray)

    private fun buildSwapData(receiveAddress: Address, tradeData: TradeData): SwapData {
        val trade = tradeData.trade

        val tokenIn = trade.tokenAmountIn.token
        val tokenOut = trade.tokenAmountOut.token

        val path = trade.route.path.map { it.address }
        val to = tradeData.options.recipient ?: receiveAddress
        val deadline = (Date().time / 1000 + tradeData.options.ttl).toBigInteger()

        val method = when (trade.type) {
            TradeType.ExactOut -> buildMethodForExactOut(tokenIn, tokenOut, path, to, deadline, tradeData, trade)
            TradeType.ExactIn -> buildMethodForExactIn(tokenIn, tokenOut, path, to, deadline, tradeData, trade)
        }

        val amount = if (tokenIn.isEther) {
            when (trade.type) {
                TradeType.ExactIn -> trade.tokenAmountIn.rawAmount
                TradeType.ExactOut -> tradeData.tokenAmountInMax.rawAmount
            }
        } else {
            BigInteger.ZERO
        }

        return SwapData(amount, method.encodedABI())
    }

    private fun buildMethodForExactOut(tokenIn: Token, tokenOut: Token, path: List<Address>, to: Address, deadline: BigInteger, tradeData: TradeData, trade: Trade): ContractMethod {
        val amountInMax = tradeData.tokenAmountInMax.rawAmount
        val amountOut = trade.tokenAmountOut.rawAmount

        return when {
            tokenIn is Ether && tokenOut is Erc20 -> SwapETHForExactTokensMethod(amountOut, path, to, deadline)
            tokenIn is Erc20 && tokenOut is Ether -> SwapTokensForExactETHMethod(amountOut, amountInMax, path, to, deadline)
            tokenIn is Erc20 && tokenOut is Erc20 -> SwapTokensForExactTokensMethod(amountOut, amountInMax, path, to, deadline)
            else -> throw Exception("Invalid tokenIn/Out for swap!")
        }
    }

    private fun buildMethodForExactIn(tokenIn: Token, tokenOut: Token, path: List<Address>, to: Address, deadline: BigInteger, tradeData: TradeData, trade: Trade): ContractMethod {
        val amountIn = trade.tokenAmountIn.rawAmount
        val amountOutMin = tradeData.tokenAmountOutMin.rawAmount

        return when {
            tokenIn is Ether && tokenOut is Erc20 -> SwapExactETHForTokensMethod(amountOutMin, path, to, deadline)
            tokenIn is Erc20 && tokenOut is Ether -> SwapExactTokensForETHMethod(amountIn, amountOutMin, path, to, deadline)
            tokenIn is Erc20 && tokenOut is Erc20 -> SwapExactTokensForTokensMethod(amountIn, amountOutMin, path, to, deadline)
            else -> throw Exception("Invalid tokenIn/Out for swap!")
        }
    }

    companion object {

        fun getRouterAddress(chain: Chain) =
            when (chain) {
                Chain.Ethereum, Chain.EthereumGoerli -> Address("0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D")
                Chain.BinanceSmartChain -> Address("0x10ED43C718714eb63d5aA57B78B54704E256024E")
                Chain.Polygon -> Address("0xa5E0829CaCEd8fFDD4De3c43696c57F7D7A678ff")
                Chain.Avalanche -> Address("0x60aE616a2155Ee3d9A68541Ba4544862310933d4")
                Chain.Base -> Address("0x4752ba5DBc23f44D87826276BF6Fd6b1C372aD24")
                else -> throw UnsupportedChainError.NoRouterAddress
            }

        private fun getFactoryAddressString(chain: Chain) =
            when (chain) {
                Chain.Ethereum, Chain.EthereumGoerli -> "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f"
                Chain.BinanceSmartChain -> "0xcA143Ce32Fe78f1f7019d7d551a6402fC5350c73"
                Chain.Polygon -> "0x5757371414417b8C6CAad45bAeF941aBc7d3Ab32"
                Chain.Avalanche -> "0x9Ad6C38BE94206cA50bb0d90783181662f0Cfa10"
                Chain.Base -> "0x8909Dc15e40173Ff4699343b6eB8132c65e18eC6"
                else -> throw UnsupportedChainError.NoFactoryAddress
            }

        private fun getInitCodeHashString(chain: Chain) =
            when (chain) {
                Chain.Ethereum,
                Chain.EthereumGoerli,
                Chain.Polygon,
                Chain.Avalanche,
                Chain.Base,
                -> "0x96e8ac4277198ff8b6f785478aa9a39f403cb768dd02cbee326c3e7da348845f"

                Chain.BinanceSmartChain -> "0x00fb7f630766e6a796048ea87d01acd3068e8ff67d078148a3fa3f4a84f69bd5"
                else -> throw UnsupportedChainError.NoInitCodeHash
            }

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
