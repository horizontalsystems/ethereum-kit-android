package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.uniswapkit.UniswapKitError
import java.math.BigInteger

class Pair(
        private val tokenAmount0: TokenAmount,
        private val tokenAmount1: TokenAmount
) {
    val token0 = tokenAmount0.token
    val token1 = tokenAmount1.token
    val reserve0 = tokenAmount0.amount
    val reserve1 = tokenAmount1.amount

    private fun reserve(token: Token): BigInteger {
        return if (token == token0) reserve0 else reserve1
    }

    fun other(token: Token): Token {
        return if (token == token0) token1 else token0
    }

    fun involves(token: Token): Boolean {
        return token == token0 || token == token1
    }

    fun tokenAmountOut(tokenAmountIn: TokenAmount): TokenAmount {
        //todo validations

        val tokenIn = tokenAmountIn.token
        val tokenOut = other(tokenIn)

        val reserveIn = reserve(tokenIn)
        val reserveOut = reserve(tokenOut)

        val amountInWithFee = tokenAmountIn.amount * BigInteger.valueOf(997)
        val numerator = amountInWithFee * reserveOut
        val denominator = reserveIn * BigInteger.valueOf(1000) + amountInWithFee
        val amountOut = numerator / denominator

        return TokenAmount(tokenOut, amountOut)
    }

    fun tokenAmountIn(tokenAmountOut: TokenAmount): TokenAmount {
        //todo validations

        val amountOut = tokenAmountOut.amount

        val tokenOut = tokenAmountOut.token
        val tokenIn = other(tokenOut)

        val reserveOut = reserve(tokenOut)
        val reserveIn = reserve(tokenIn)

        check(amountOut < reserveOut) {
            throw UniswapKitError.InsufficientReserve()
        }

        val numerator = reserveIn * amountOut * BigInteger.valueOf(1000)
        val denominator = (reserveOut - amountOut) * BigInteger.valueOf(997)
        val amountIn = numerator / denominator + BigInteger.ONE

        return TokenAmount(tokenIn, amountIn)
    }

    override fun toString(): String {
        return "Pair {$tokenAmount0, $tokenAmount1}"
    }

    companion object {
        fun address(token0: Token, token1: Token): ByteArray {
            val data = "0xff".hexStringToByteArray() +
                    "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f".hexStringToByteArray() +
                    CryptoUtils.sha3(token0.address + token1.address) +
                    "0x96e8ac4277198ff8b6f785478aa9a39f403cb768dd02cbee326c3e7da348845f".hexStringToByteArray()

            return CryptoUtils.sha3(data).copyOfRange(12, 32)
        }
    }

}
