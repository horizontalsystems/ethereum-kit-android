package io.horizontalsystems.oneinchkit

import com.google.gson.GsonBuilder
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.network.*
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigInteger
import java.util.logging.Logger

class OneInchService(
    chain: Chain
) {
    private val logger = Logger.getLogger("OneInchService")
    private val url = "https://unstoppable.api.enterprise.1inch.exchange/v5.0/${chain.id}/"
    private val service: OneInchServiceApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            logger.info(message)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        val gson = GsonBuilder()
            .setLenient()
            .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter(isHex = false))
            .registerTypeAdapter(Long::class.java, LongTypeAdapter(isHex = false))
            .registerTypeAdapter(Int::class.java, IntTypeAdapter(isHex = false))
            .registerTypeAdapter(ByteArray::class.java, ByteArrayTypeAdapter())
            .registerTypeAdapter(Address::class.java, AddressTypeAdapter())
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        service = retrofit.create(OneInchServiceApi::class.java)
    }

    fun getApproveCallDataAsync(tokenAddress: Address, amount: BigInteger) =
        service.getApproveCallData(tokenAddress.hex, amount)

    fun getQuoteAsync(
        fromToken: Address,
        toToken: Address,
        amount: BigInteger,
        protocols: List<String>? = null,
        gasPrice: GasPrice? = null,
        complexityLevel: Int? = null,
        connectorTokens: List<String>? = null,
        gasLimit: Long? = null,
        mainRouteParts: Int? = null,
        parts: Int? = null
    ) = if (gasPrice is GasPrice.Eip1559) {
        service.getQuote(
            fromTokenAddress = fromToken.hex,
            toTokenAddress = toToken.hex,
            amount = amount,
            protocols = protocols?.joinToString(","),
            maxFeePerGas = gasPrice.maxFeePerGas,
            maxPriorityFeePerGas = gasPrice.maxPriorityFeePerGas,
            complexityLevel = complexityLevel,
            connectorTokens = connectorTokens?.joinToString(","),
            gasLimit = gasLimit,
            parts = parts,
            mainRouteParts = mainRouteParts
        )
    } else {
        service.getQuote(
            fromTokenAddress = fromToken.hex,
            toTokenAddress = toToken.hex,
            amount = amount,
            protocols = protocols?.joinToString(","),
            gasPrice = gasPrice?.max,
            complexityLevel = complexityLevel,
            connectorTokens = connectorTokens?.joinToString(","),
            gasLimit = gasLimit,
            parts = parts,
            mainRouteParts = mainRouteParts
        )
    }

    fun getSwapAsync(
        fromTokenAddress: Address,
        toTokenAddress: Address,
        amount: BigInteger,
        fromAddress: Address,
        slippagePercentage: Float,
        protocols: List<String>? = null,
        recipient: Address? = null,
        gasPrice: GasPrice? = null,
        burnChi: Boolean? = null,
        complexityLevel: Int? = null,
        connectorTokens: List<String>? = null,
        allowPartialFill: Boolean? = null,
        gasLimit: Long? = null,
        parts: Int? = null,
        mainRouteParts: Int? = null
    ) = if (gasPrice is GasPrice.Eip1559) {
        service.getSwap(
            fromTokenAddress = fromTokenAddress.hex,
            toTokenAddress = toTokenAddress.hex,
            amount = amount,
            fromAddress = fromAddress.hex,
            slippagePercentage = slippagePercentage,
            protocols = protocols?.joinToString(","),
            recipient = recipient?.hex,
            maxFeePerGas = gasPrice.maxFeePerGas,
            maxPriorityFeePerGas = gasPrice.maxPriorityFeePerGas,
            burnChi = burnChi,
            complexityLevel = complexityLevel,
            connectorTokens = connectorTokens?.joinToString(","),
            allowPartialFill = allowPartialFill,
            gasLimit = gasLimit,
            parts = parts,
            mainRouteParts = mainRouteParts
        )
    } else {
        service.getSwap(
            fromTokenAddress = fromTokenAddress.hex,
            toTokenAddress = toTokenAddress.hex,
            amount = amount,
            fromAddress = fromAddress.hex,
            slippagePercentage = slippagePercentage,
            protocols = protocols?.joinToString(","),
            recipient = recipient?.hex,
            gasPrice = gasPrice?.max,
            burnChi = burnChi,
            complexityLevel = complexityLevel,
            connectorTokens = connectorTokens?.joinToString(","),
            allowPartialFill = allowPartialFill,
            gasLimit = gasLimit,
            parts = parts,
            mainRouteParts = mainRouteParts
        )
    }

    private interface OneInchServiceApi {
        @GET("approve/calldata")
        fun getApproveCallData(
            @Query("tokenAddress") tokenAddress: String,
            @Query("amount") amount: BigInteger? = null,
            @Query("infinity") infinity: Boolean? = null
        ): Single<ApproveCallData>

        @GET("quote")
        fun getQuote(
            @Query("fromTokenAddress") fromTokenAddress: String,
            @Query("toTokenAddress") toTokenAddress: String,
            @Query("amount") amount: BigInteger,
            @Query("protocols") protocols: String? = null,
            @Query("maxFeePerGas") maxFeePerGas: Long? = null,
            @Query("maxPriorityFeePerGas") maxPriorityFeePerGas: Long? = null,
            @Query("gasPrice") gasPrice: Long? = null,
            @Query("complexityLevel") complexityLevel: Int? = null,
            @Query("connectorTokens") connectorTokens: String? = null,
            @Query("gasLimit") gasLimit: Long? = null,
            @Query("parts") parts: Int? = null,
            @Query("mainRouteParts") mainRouteParts: Int? = null
        ): Single<Quote>

        @GET("swap")
        fun getSwap(
            @Query("fromTokenAddress") fromTokenAddress: String,
            @Query("toTokenAddress") toTokenAddress: String,
            @Query("amount") amount: BigInteger,
            @Query("fromAddress") fromAddress: String,
            @Query("slippage") slippagePercentage: Float,
            @Query("protocols") protocols: String? = null,
            @Query("destReceiver") recipient: String? = null,
            @Query("maxFeePerGas") maxFeePerGas: Long? = null,
            @Query("maxPriorityFeePerGas") maxPriorityFeePerGas: Long? = null,
            @Query("gasPrice") gasPrice: Long? = null,
            @Query("burnChi") burnChi: Boolean? = null,
            @Query("complexityLevel ") complexityLevel: Int? = null,
            @Query("connectorTokens") connectorTokens: String? = null,
            @Query("allowPartialFill") allowPartialFill: Boolean? = null,
            @Query("gasLimit") gasLimit: Long? = null,
            @Query("parts") parts: Int? = null,
            @Query("mainRouteParts") mainRouteParts: Int? = null
        ): Single<Swap>
    }

}
