package io.horizontalsystems.oneinchkit

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.oneinchkit.contracts.OneInchContractMethodFactories
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecorator
import io.horizontalsystems.oneinchkit.decorations.OneInchTransactionDecorator
import java.math.BigInteger
import java.util.Objects

class OneInchKit(
    private val service: OneInchService
) {

    fun getApproveCallDataAsync(chain: Chain, tokenAddress: Address, amount: BigInteger) =
        service.getApproveCallDataAsync(chain, tokenAddress, amount)

    fun getQuoteAsync(
        chain: Chain,
        fromToken: Address,
        toToken: Address,
        amount: BigInteger,
        fee: Float? = null,
        protocols: List<String>? = null,
        gasPrice: GasPrice? = null,
        complexityLevel: Int? = null,
        connectorTokens: List<String>? = null,
        gasLimit: Long? = null,
        mainRouteParts: Int? = null,
        parts: Int? = null
    ) = service.getQuoteAsync(
        chain,
        fromToken,
        toToken,
        amount,
        fee,
        protocols,
        gasPrice,
        complexityLevel,
        connectorTokens,
        gasLimit,
        mainRouteParts,
        parts
    )

    fun getSwapAsync(
        chain: Chain,
        receiveAddress: Address,
        fromToken: Address,
        toToken: Address,
        amount: BigInteger,
        slippagePercentage: Float,
        referrer: String? = null,
        fee: Float? = null,
        protocols: List<String>? = null,
        recipient: Address? = null,
        gasPrice: GasPrice? = null,
        burnChi: Boolean = false,
        complexityLevel: Int? = null,
        connectorTokens: List<String>? = null,
        allowPartialFill: Boolean = false,
        gasLimit: Long? = null,
        parts: Int? = null,
        mainRouteParts: Int? = null
    ) = service.getSwapAsync(
        chain,
        fromToken,
        toToken,
        amount,
        receiveAddress,
        slippagePercentage,
        referrer,
        fee,
        protocols,
        recipient,
        gasPrice,
        burnChi,
        complexityLevel,
        connectorTokens,
        allowPartialFill,
        gasLimit,
        parts,
        mainRouteParts
    )

    companion object {
        fun getInstance(apiKey: String): OneInchKit {
            return OneInchKit(OneInchService(apiKey))
        }

        fun addDecorators(evmKit: EthereumKit) {
            evmKit.addMethodDecorator(OneInchMethodDecorator(OneInchContractMethodFactories))
            evmKit.addTransactionDecorator(OneInchTransactionDecorator(evmKit.receiveAddress))
        }

        fun routerAddress(chain: Chain): Address = when (chain) {
            Chain.Ethereum,
            Chain.BinanceSmartChain,
            Chain.Polygon,
            Chain.Optimism,
            Chain.ArbitrumOne,
            Chain.Gnosis,
            Chain.Fantom,
            Chain.Avalanche,
            Chain.Base,
            -> Address("0x1111111254EEB25477B68fb85Ed929f73A960582")

            else -> throw IllegalArgumentException("Invalid Chain: ${chain.id}")
        }
    }

}

data class Token(
    val symbol: String,
    val name: String,
    val decimals: Int,
    val address: String,
    val logoURI: String
)

data class Quote(
    val fromToken: Token,
    val toToken: Token,
    @SerializedName("toAmount") val toTokenAmount: BigInteger,
    @SerializedName("protocols") val route: List<Any>,
    @SerializedName("gas") val estimatedGas: Long
) {
    override fun toString(): String {
        return "Quote {fromToken: ${fromToken.name}, toToken: ${toToken.name}, toTokenAmount: $toTokenAmount}"
    }
}

data class SwapTransaction(
    val from: Address,
    val to: Address,
    val data: ByteArray,
    val value: BigInteger,
    val gasPrice: Long?,
    val maxFeePerGas: Long?,
    val maxPriorityFeePerGas: Long?,
    @SerializedName("gas") val gasLimit: Long
) {
    override fun toString(): String {
        return "SwapTransaction {\nfrom: ${from.hex}, \nto: ${to.hex}, \ndata: ${data.toHexString()}, \nvalue: $value, \ngasPrice: $gasPrice, \ngasLimit: $gasLimit\n}"
    }
}

data class Swap(
    val fromToken: Token,
    val toToken: Token,
    val fromTokenAmount: BigInteger,
    @SerializedName("toAmount") val toTokenAmount: BigInteger,
    @SerializedName("protocols") val route: List<Any>,
    @SerializedName("tx") val transaction: SwapTransaction
) {
    override fun toString(): String {
        return "Swap {\nfromToken: ${fromToken.name}, \ntoToken: ${toToken.name}, \nfromTokenAmount: $fromTokenAmount, \ntoTokenAmount: $toTokenAmount, \ntx: $transaction\n}"
    }
}

data class ApproveCallData(
    val data: ByteArray,
    val gasPrice: Long,
    val to: Address,
    val value: BigInteger
) {
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is ApproveCallData -> to == other.to && value == other.value && data.contentEquals(other.data)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(to, value, data)
    }

    override fun toString(): String {
        return "ApproveCallData {\nto: ${to.hex}, \nvalue: $value, \ndata: ${data.toHexString()}\n}"
    }
}
