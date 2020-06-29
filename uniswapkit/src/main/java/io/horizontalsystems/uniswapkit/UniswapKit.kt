package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.uniswapkit.SwapItem.Erc20SwapItem
import io.horizontalsystems.uniswapkit.SwapItem.EthereumSwapItem
import io.reactivex.Single
import java.math.BigInteger
import java.util.*

class UniswapKit(
        private val ethereumKit: EthereumKit,
        private val tradeManager: TradeManager
) {

    fun wethAddress(): Single<ByteArray> {
        return tradeManager.wethAddress()
    }

    fun factoryAddress(): Single<ByteArray> {
        return tradeManager.factoryAddress()
    }

    fun getAmountsOut(amountIn: String, fromItem: SwapItem, toItem: SwapItem): Single<List<PathItem>> {
        return try {
            val path = generatePath(fromItem, toItem)
            tradeManager.getAmountsOut(convertAmount(amountIn), path).map { amounts ->
                getPathItems(path, amounts)
            }
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    fun getAmountsIn(amountOut: String, fromItem: SwapItem, toItem: SwapItem): Single<List<PathItem>> {
        return try {
            val path = generatePath(fromItem, toItem)
            tradeManager.getAmountsIn(convertAmount(amountOut), path).map { amounts ->
                getPathItems(path, amounts)
            }
        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    fun swapExactItemForItem(pathItems: List<PathItem>): Single<String> {
        try {
            val path = pathItems.map { address(it.swapItem) }
            val fromPathItem = pathItems.firstOrNull() ?: throw UniswapKitError.InvalidPathItems()
            val toPathItem = pathItems.lastOrNull() ?: throw UniswapKitError.InvalidPathItems()
            val amountIn = convertAmount(fromPathItem.amount)
            val amountOutMin = convertAmount(toPathItem.amount)

            return when {
                fromPathItem.swapItem is EthereumSwapItem && toPathItem.swapItem is Erc20SwapItem -> {
                    tradeManager.swapExactETHForTokens(amountIn, amountOutMin, path)
                }
                fromPathItem.swapItem is Erc20SwapItem && toPathItem.swapItem is EthereumSwapItem -> {
                    tradeManager.swapExactTokensForETH(amountIn, amountOutMin, path)
                }
                fromPathItem.swapItem is Erc20SwapItem && toPathItem.swapItem is Erc20SwapItem -> {
                    tradeManager.swapExactTokensForTokens(amountIn, amountOutMin, path)
                }
                else -> throw Exception("Invalid swap pairs!")
            }
        } catch (error: Throwable) {
            return Single.error(error)
        }
    }

    fun swapItemForExactItem(pathItems: List<PathItem>): Single<String> {
        try {
            val path = pathItems.map { address(it.swapItem) }
            val fromPathItem = pathItems.firstOrNull() ?: throw UniswapKitError.InvalidPathItems()
            val toPathItem = pathItems.lastOrNull() ?: throw UniswapKitError.InvalidPathItems()
            val amountInMax = convertAmount(fromPathItem.amount)
            val amountOut = convertAmount(toPathItem.amount)

            return when {
                fromPathItem.swapItem is EthereumSwapItem && toPathItem.swapItem is Erc20SwapItem -> {
                    tradeManager.swapETHForExactTokens(amountOut, amountInMax, path)
                }
                fromPathItem.swapItem is Erc20SwapItem && toPathItem.swapItem is EthereumSwapItem -> {
                    tradeManager.swapTokensForExactETH(amountOut, amountInMax, path)
                }
                fromPathItem.swapItem is Erc20SwapItem && toPathItem.swapItem is Erc20SwapItem -> {
                    tradeManager.swapTokensForExactTokens(amountOut, amountInMax, path)
                }
                else -> throw Exception("Invalid swap pairs!")
            }
        } catch (error: Throwable) {
            return Single.error(error)
        }
    }

    @Throws(UniswapKitError.InvalidAddress::class)
    private fun convertAddress(address: String): ByteArray {
        try {
            return address.hexStringToByteArray()
        } catch (e: Exception) {
            throw UniswapKitError.InvalidAddress()
        }
    }

    @Throws(UniswapKitError.InvalidAmount::class)
    private fun convertAmount(value: String): BigInteger {
        try {
            return value.toBigInteger()
        } catch (e: Exception) {
            throw UniswapKitError.InvalidAmount()
        }
    }

    private fun address(item: SwapItem): ByteArray {
        return when (item) {
            is EthereumSwapItem -> wethAddress
            is Erc20SwapItem -> convertAddress(item.contractAddress)
        }
    }

    private fun generatePath(fromItem: SwapItem, toItem: SwapItem): List<ByteArray> {
        val fromAddress = address(fromItem)
        val toAddress = address(toItem)

        return if (fromItem is Erc20SwapItem && toItem is Erc20SwapItem) {
            listOf(fromAddress, wethAddress, toAddress)
        } else {
            listOf(fromAddress, toAddress)
        }
    }

    private fun getPathItems(path: List<ByteArray>, amounts: List<BigInteger>): List<PathItem> {
        return amounts.mapIndexed { index, amount ->
            val address = path[index]
            val swapItem = if (address.contentEquals(wethAddress)) EthereumSwapItem() else Erc20SwapItem(address.toHexString())
            PathItem(swapItem, amount.toString())
        }
    }

    companion object {
        private val routerAddress = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D".hexStringToByteArray()
        private val wethAddress = "0xc778417e063141139fce010982780140aa0cd5ab".hexStringToByteArray()

        fun getInstance(ethereumKit: EthereumKit): UniswapKit {
            val address = ethereumKit.receiveAddressRaw
            val tradeManager = TradeManager(ethereumKit, routerAddress, address)
            return UniswapKit(ethereumKit, tradeManager)
        }
    }

}

sealed class UniswapKitError : Throwable() {
    class InvalidAmount : UniswapKitError()
    class InvalidAddress : UniswapKitError()
    class InvalidPathItems : UniswapKitError()
}

sealed class SwapItem {
    class EthereumSwapItem : SwapItem()
    class Erc20SwapItem(val contractAddress: String) : SwapItem()

    override fun equals(other: Any?): Boolean {
        if (other !is SwapItem)
            return false

        if (other.javaClass != this.javaClass)
            return false

        if (other is Erc20SwapItem && this is Erc20SwapItem) {
            return other.contractAddress == this.contractAddress
        }

        return true
    }

    override fun hashCode(): Int {
        if (this is Erc20SwapItem) {
            return Objects.hashCode(this.contractAddress)
        }
        return Objects.hashCode(this.javaClass.name)
    }
}

data class PathItem(val swapItem: SwapItem, val amount: String)
