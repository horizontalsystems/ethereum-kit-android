package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import java.math.BigInteger
import kotlin.math.max

object UniswapABI {

    fun weth(): ByteArray {
        return methodId("WETH()")
    }

    fun factory(): ByteArray {
        return methodId("factory()")
    }

    fun getAmountsOut(amountIn: BigInteger, path: List<ByteArray>): ByteArray {
        val methodId = methodId("getAmountsOut(uint256,address[])")
        return methodId +
                pad(amountIn.toByteArray()) +
                pad(BigInteger.valueOf(2 * 32).toByteArray()) +
                encode(path)
    }

    fun getAmountsIn(amountOut: BigInteger, path: List<ByteArray>): ByteArray {
        val methodId = methodId("getAmountsIn(uint256,address[])")
        return methodId +
                pad(amountOut.toByteArray()) +
                pad(BigInteger.valueOf(2 * 32).toByteArray()) +
                encode(path)
    }

    fun swapExactETHForTokens(amountOutMin: BigInteger, path: List<ByteArray>, to: ByteArray, deadline: BigInteger): ByteArray {
        val methodId = methodId("swapExactETHForTokens(uint256,address[],address,uint256)")
        return methodId +
                pad(amountOutMin.toByteArray()) +
                pad(BigInteger.valueOf(4 * 32).toByteArray()) +
                pad(to) +
                pad(deadline.toByteArray()) +
                encode(path)
    }

    fun swapTokensForExactETH(amountOut: BigInteger, amountInMax: BigInteger, path: List<ByteArray>, to: ByteArray, deadline: BigInteger): ByteArray {
        val methodId = methodId("swapTokensForExactETH(uint256,uint256,address[],address,uint256)")
        return methodId +
                pad(amountOut.toByteArray()) +
                pad(amountInMax.toByteArray()) +
                pad(BigInteger.valueOf(5 * 32).toByteArray()) +
                pad(to) +
                pad(deadline.toByteArray()) +
                encode(path)
    }

    fun swapExactTokensForETH(amountIn: BigInteger, amountOutMin: BigInteger, path: List<ByteArray>, to: ByteArray, deadline: BigInteger): ByteArray {
        val methodId = methodId("swapExactTokensForETH(uint256,uint256,address[],address,uint256)")
        return methodId +
                pad(amountIn.toByteArray()) +
                pad(amountOutMin.toByteArray()) +
                pad(BigInteger.valueOf(5 * 32).toByteArray()) +
                pad(to) +
                pad(deadline.toByteArray()) +
                encode(path)
    }

    fun swapETHForExactTokens(amountOut: BigInteger, path: List<ByteArray>, to: ByteArray, deadline: BigInteger): ByteArray {
        val methodId = methodId("swapETHForExactTokens(uint256,address[],address,uint256)")
        return methodId +
                pad(amountOut.toByteArray()) +
                pad(BigInteger.valueOf(4 * 32).toByteArray()) +
                pad(to) +
                pad(deadline.toByteArray()) +
                encode(path)
    }

    fun swapExactTokensForTokens(amountIn: BigInteger, amountOutMin: BigInteger, path: List<ByteArray>, to: ByteArray, deadline: BigInteger): ByteArray {
        val methodId = methodId("swapExactTokensForTokens(uint256,uint256,address[],address,uint256)")
        return methodId +
                pad(amountIn.toByteArray()) +
                pad(amountOutMin.toByteArray()) +
                pad(BigInteger.valueOf(5 * 32).toByteArray()) +
                pad(to) +
                pad(deadline.toByteArray()) +
                encode(path)
    }

    fun swapTokensForExactTokens(amountOut: BigInteger, amountInMax: BigInteger, path: List<ByteArray>, to: ByteArray, deadline: BigInteger): ByteArray {
        val methodId = methodId("swapTokensForExactTokens(uint256,uint256,address[],address,uint256)")
        return methodId +
                pad(amountOut.toByteArray()) +
                pad(amountInMax.toByteArray()) +
                pad(BigInteger.valueOf(5 * 32).toByteArray()) +
                pad(to) +
                pad(deadline.toByteArray()) +
                encode(path)
    }

    private fun methodId(methodSignature: String): ByteArray {
        val hash = CryptoUtils.sha3(methodSignature.toByteArray())
        return hash.copyOfRange(0, 4)
    }

    private fun pad(data: ByteArray): ByteArray {
        val prePadding = ByteArray(max(0, 32 - data.size))
        return prePadding + data
    }

    private fun encode(path: List<ByteArray>): ByteArray {
        var data = pad(BigInteger.valueOf(path.size.toLong()).toByteArray())
        for (address in path) {
            data += pad(address)
        }
        return data
    }

}
