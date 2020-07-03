package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Single
import java.math.BigInteger
import java.util.*
import java.util.logging.Logger

class TradeManager(
        private val ethereumKit: EthereumKit,
        private val routerAddress: ByteArray
) {

    private val address: ByteArray = ethereumKit.receiveAddressRaw
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    fun wethAddress(): Single<ByteArray> {
        return ethereumKit.call(routerAddress, UniswapABI.weth())
                .map { wethAddress ->
                    wethAddress.dropWhile { it.compareTo(0) == 0 }.toByteArray()
                }
    }

    fun factoryAddress(): Single<ByteArray> {
        return ethereumKit.call(routerAddress, UniswapABI.factory())
                .map { factoryAddress ->
                    factoryAddress.dropWhile { it.compareTo(0) == 0 }.toByteArray()
                }
    }

    fun getAmountsOut(amountInt: BigInteger, path: List<ByteArray>): Single<List<BigInteger>> {
        val transactionInput = UniswapABI.getAmountsOut(amountInt, path)
        return ethereumKit.call(routerAddress, transactionInput).map { decodeAmounts(it, path.size) }
    }

    fun getAmountsIn(amountOut: BigInteger, path: List<ByteArray>): Single<List<BigInteger>> {
        val transactionInput = UniswapABI.getAmountsIn(amountOut, path)
        return ethereumKit.call(routerAddress, transactionInput).map { decodeAmounts(it, path.size) }
    }

    fun swapExactETHForTokens(amountIn: BigInteger, amountOutMin: BigInteger, path: List<ByteArray>): Single<String> {
        val transactionInput = UniswapABI.swapExactETHForTokens(amountOutMin, path, address, deadline)
        return swap(amountIn, transactionInput)
    }

    fun swapTokensForExactETH(amountOut: BigInteger, amountInMax: BigInteger, path: List<ByteArray>): Single<String> {
        val transactionInput = UniswapABI.swapTokensForExactETH(amountOut, amountInMax, path, address, deadline)
        return swapWithApprove(path[0], amountInMax, swap(BigInteger.ZERO, transactionInput))
    }

    fun swapExactTokensForETH(amountIn: BigInteger, amountOutMin: BigInteger, path: List<ByteArray>): Single<String> {
        val transactionInput = UniswapABI.swapExactTokensForETH(amountIn, amountOutMin, path, address, deadline)
        return swapWithApprove(path[0], amountIn, swap(BigInteger.ZERO, transactionInput))
    }

    fun swapETHForExactTokens(amountOut: BigInteger, amountInMax: BigInteger, path: List<ByteArray>): Single<String> {
        val transactionInput = UniswapABI.swapETHForExactTokens(amountOut, path, address, deadline)
        return swap(amountInMax, transactionInput)
    }

    fun swapExactTokensForTokens(amountIn: BigInteger, amountOutMin: BigInteger, path: List<ByteArray>): Single<String> {
        val transactionInput = UniswapABI.swapExactTokensForTokens(amountIn, amountOutMin, path, address, deadline)
        return swapWithApprove(path[0], amountIn, swap(BigInteger.ZERO, transactionInput))
    }

    fun swapTokensForExactTokens(amountOut: BigInteger, amountInMax: BigInteger, path: List<ByteArray>): Single<String> {
        val transactionInput = UniswapABI.swapTokensForExactTokens(amountOut, amountInMax, path, address, deadline)
        return swapWithApprove(path[0], amountInMax, swap(BigInteger.ZERO, transactionInput))
    }

    private val deadline: BigInteger
        get() = (Date().time / 1000 + 3600).toBigInteger()

    private fun swap(value: BigInteger, input: ByteArray): Single<String> {
        return ethereumKit.send(routerAddress, value, input, 50_000_000_000, 500_000)
                .map { txInfo ->
                    logger.info("Swap tx hash: ${txInfo.hash}")
                    txInfo.hash
                }
    }

    private fun swapWithApprove(contractAddress: ByteArray, amount: BigInteger, swapSingle: Single<String>): Single<String> {
        val approveTransactionInput = Erc20ABI.approve(routerAddress, amount)
        return ethereumKit.send(contractAddress, BigInteger.ZERO, approveTransactionInput, 50_000_000_000, 500_000)
                .flatMap { txInfo ->
                    logger.info("Approve tx hash: ${txInfo.hash}")
                    swapSingle
                }
    }

    private fun decodeAmounts(data: ByteArray, pathCount: Int): List<BigInteger> {
        check(data.size == 64 + pathCount * 32) {
            throw Exception("Invalid amounts")
        }
        return IntRange(0, pathCount - 1).map { i ->
            val startIndex = 64 + i * 32
            val endIndex = startIndex + 32
            BigInteger(data.sliceArray(startIndex until endIndex))
        }
    }

}
