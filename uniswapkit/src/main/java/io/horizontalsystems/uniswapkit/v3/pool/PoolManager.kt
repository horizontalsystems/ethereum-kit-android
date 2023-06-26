package io.horizontalsystems.uniswapkit.v3.pool

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.Fraction
import io.horizontalsystems.uniswapkit.v3.FeeAmount
import kotlinx.coroutines.rx2.await
import java.math.BigInteger

class PoolManager(
    private val ethereumKit: EthereumKit,
    dexType: DexType
) {
    private val factoryAddress = when (dexType) {
        DexType.Uniswap -> getUniswapFactoryAddress(ethereumKit.chain)
        DexType.PancakeSwap -> getPancakeSwapFactoryAddress(ethereumKit.chain)
    }

    private fun getUniswapFactoryAddress(chain: Chain)= when (chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0x1F98431c8aD98523631AE4a59f267346ea31F984"
        Chain.BinanceSmartChain -> "0xdB1d10011AD0Ff90774D0C6Bb92e5C5c8b4461F7"
        else -> throw IllegalStateException("Not supported Uniswap chain ${ethereumKit.chain}")
    }

    private fun getPancakeSwapFactoryAddress(chain: Chain)= when (chain) {
        Chain.BinanceSmartChain,
        Chain.Ethereum -> "0x0BFbCF9fa4f9C56B0F40a671Ad40E0805A091865"
        else -> throw IllegalStateException("Not supported PancakeSwap chain ${ethereumKit.chain}")
    }

    // get price of tokenA in tokenB
    suspend fun getPoolPrice(tokenA: Address, tokenB: Address, fee: FeeAmount): Fraction {
        val poolAddress = getPoolAddress(tokenA, tokenB, fee)
        val callResponse = ethCall(poolAddress, Slot0Method().encodedABI())
        val sqrtPriceX96 = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()

        val price = Fraction(sqrtPriceX96.pow(2), BigInteger.valueOf(2).pow(192))
        return when {
            tokenA.hex < tokenB.hex -> price
            else -> price.invert()
        }
    }

    private suspend fun getPoolAddress(tokenA: Address, tokenB: Address, fee: FeeAmount): Address {
        val callResponse = ethCall(Address(factoryAddress), GetPoolMethod(tokenA, tokenB, fee.value).encodedABI())
        return Address(callResponse.sliceArray(IntRange(0, 31)))
    }

    private suspend fun ethCall(contractAddress: Address, data: ByteArray): ByteArray {
        return ethereumKit.call(contractAddress, data).await()
    }
}

