package io.horizontalsystems.oneinchkit

import com.google.gson.GsonBuilder
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Address
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
        networkType: NetworkType
) {
    private val logger = Logger.getLogger("OneInchService")
    private val url = "https://api.1inch.exchange/v3.0/${networkType.chainId}/"
    private val service: OneInchServiceApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)

        val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter(isHex = false))
                .registerTypeAdapter(Long::class.java, LongTypeAdapter())
                .registerTypeAdapter(Int::class.java, IntTypeAdapter())
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

    fun getApproveCallDataAsync(tokenAddress: Address, amount: BigInteger): Single<ApproveCallData> {
        return service.getApproveCallData(tokenAddress.hex, amount)
    }

    fun getQuoteAsync(
            fromToken: Address,
            toToken: Address,
            amount: BigInteger,
            protocols: List<String>? = null,
            gasPrice: Long? = null,
            complexityLevel: Int? = null,
            connectorTokens: List<String>? = null,
            gasLimit: Long? = null,
            mainRouteParts: Int? = null,
            parts: Int? = null
    ): Single<Quote> {
        return service.getQuote(fromToken.hex, toToken.hex, amount, protocols?.joinToString(","), gasPrice, complexityLevel, connectorTokens?.joinToString(","), gasLimit, parts, mainRouteParts)
    }

    fun getSwapAsync(
            fromTokenAddress: Address,
            toTokenAddress: Address,
            amount: BigInteger,
            fromAddress: Address,
            slippagePercentage: Float,
            protocols: List<String>? = null,
            recipient: Address? = null,
            gasPrice: Long? = null,
            burnChi: Boolean? = null,
            complexityLevel: Int? = null,
            connectorTokens: List<String>? = null,
            allowPartialFill: Boolean? = null,
            gasLimit: Long? = null,
            parts: Int? = null,
            mainRouteParts: Int? = null
    ): Single<Swap> {
        return service.getSwap(fromTokenAddress.hex, toTokenAddress.hex, amount, fromAddress.hex, slippagePercentage, protocols?.joinToString(","), recipient?.hex, gasPrice, burnChi, complexityLevel, connectorTokens?.joinToString(","), allowPartialFill, gasLimit, parts, mainRouteParts)
    }

    private interface OneInchServiceApi {

        @GET("approve/calldata")
        fun getApproveCallData(
                @Query("tokenAddress") tokenAddress: String,
                @Query("amount") amount: BigInteger? = null,
                @Query("infinity") infinity: Boolean? = null
        ): Single<ApproveCallData>

        @GET("approve/spender")
        fun getApproveSpender(): Single<Spender>

        @GET("quote")
        fun getQuote(
                @Query("fromTokenAddress") fromTokenAddress: String,
                @Query("toTokenAddress") toTokenAddress: String,
                @Query("amount") amount: BigInteger,
                @Query("protocols") protocols: String? = null,
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
