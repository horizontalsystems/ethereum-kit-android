package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.PairError
import java.math.BigInteger

class Pair(
        val reserve0: TokenAmount,
        val reserve1: TokenAmount
) {
    val token0 = reserve0.token
    val token1 = reserve1.token

    private fun reserve(token: Token): TokenAmount {
        return if (token == token0) reserve0 else reserve1
    }

    fun other(token: Token): Token {
        return if (token == token0) token1 else token0
    }

    fun involves(token: Token): Boolean {
        return token == token0 || token == token1
    }

    fun tokenAmountOut(tokenAmountIn: TokenAmount): TokenAmount {
        check(involves(tokenAmountIn.token)) {
            throw PairError.NotInvolvedToken()
        }

        check(reserve0.rawAmount > BigInteger.ZERO && reserve1.rawAmount > BigInteger.ZERO) {
            throw PairError.InsufficientReserves()
        }

        val tokenIn = tokenAmountIn.token
        val tokenOut = other(tokenIn)

        val reserveIn = reserve(tokenIn)
        val reserveOut = reserve(tokenOut)

        val amountInWithFee = tokenAmountIn.rawAmount * BigInteger.valueOf(997)
        val numerator = amountInWithFee * reserveOut.rawAmount
        val denominator = reserveIn.rawAmount * BigInteger.valueOf(1000) + amountInWithFee
        val amountOut = numerator / denominator

        return TokenAmount(tokenOut, amountOut)
    }

    fun tokenAmountIn(tokenAmountOut: TokenAmount): TokenAmount {
        check(involves(tokenAmountOut.token)) {
            throw PairError.NotInvolvedToken()
        }

        check(reserve0.rawAmount > BigInteger.ZERO && reserve1.rawAmount > BigInteger.ZERO) {
            throw PairError.InsufficientReserves()
        }

        val amountOut = tokenAmountOut.rawAmount

        val tokenOut = tokenAmountOut.token
        val tokenIn = other(tokenOut)

        val reserveOut = reserve(tokenOut)
        val reserveIn = reserve(tokenIn)

        check(amountOut < reserveOut.rawAmount) {
            throw PairError.InsufficientReserveOut()
        }

        val numerator = reserveIn.rawAmount * amountOut * BigInteger.valueOf(1000)
        val denominator = (reserveOut.rawAmount - amountOut) * BigInteger.valueOf(997)
        val amountIn = numerator / denominator + BigInteger.ONE

        return TokenAmount(tokenIn, amountIn)
    }

    override fun toString(): String {
        return "Pair {$reserve0, $reserve1}"
    }

    companion object {
        fun address(token0: Token, token1: Token, networkType: NetworkType): Address {
            val factoryAddress: String
            val initCodeHash: String

            when (networkType) {
                NetworkType.EthMainNet,
                NetworkType.EthRopsten,
                NetworkType.EthKovan,
                NetworkType.EthGoerli,
                NetworkType.EthRinkeby -> {
                    factoryAddress = "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f"
                    initCodeHash = "0x96e8ac4277198ff8b6f785478aa9a39f403cb768dd02cbee326c3e7da348845f"
                }
                NetworkType.BscMainNet -> {
                    factoryAddress = "0xcA143Ce32Fe78f1f7019d7d551a6402fC5350c73"
                    initCodeHash = "0x00fb7f630766e6a796048ea87d01acd3068e8ff67d078148a3fa3f4a84f69bd5"
                }
            }

            val data = "0xff".hexStringToByteArray() +
                    factoryAddress.hexStringToByteArray() +
                    CryptoUtils.sha3(token0.address.raw + token1.address.raw) +
                    initCodeHash.hexStringToByteArray()

            return Address(CryptoUtils.sha3(data).copyOfRange(12, 32))
        }
    }

}
