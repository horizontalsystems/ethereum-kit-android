package io.horizontalsystems.ethereumkit.network

import com.google.gson.GsonBuilder
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.math.BigInteger

class InfuraService(private val networkType: NetworkType, private val apiKey: String) {

    private val service: InfuraServiceAPI

    private val baseUrl: String
        get() {
            val subdomain = when (networkType) {
                NetworkType.Ropsten -> "ropsten"
                NetworkType.Kovan -> "kovan"
                else -> "mainnet"
            }
            return "https://$subdomain.infura.io/v3/$apiKey/"
        }

    init {
        val logger = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(logger)
                .addInterceptor { chain ->
                    val requestBuilder = chain.request().newBuilder()
                    requestBuilder.header("Content-Type", "application/json")
                    requestBuilder.header("Accept", "application/json")
                    chain.proceed(requestBuilder.build())
                }

        val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraServiceAPI::class.java)
    }

    fun getLastBlockHeight(): Single<Long> {
        val request = Request("eth_blockNumber")
        return service.makeRequestForBigInteger(request).flatMap {
            returnResultOrError(it).map { blockNumber ->
                blockNumber.toLong()
            }
        }
    }

    fun getTransactionCount(address: ByteArray): Single<Long> {
        val request = Request("eth_getTransactionCount", listOf(address.toHexString(), "latest"))
        return service.makeRequestForBigInteger(request).flatMap {
            returnResultOrError(it).map { txCount ->
                txCount.toLong()
            }
        }
    }

    fun getBalance(address: ByteArray): Single<BigInteger> {
        val request = Request("eth_getBalance", listOf(address.toHexString(), "latest"))
        return service.makeRequestForBigInteger(request).flatMap {
            returnResultOrError(it)
        }
    }

    fun getBalanceErc20(address: ByteArray, contractAddress: ByteArray): Single<BigInteger> {
        val encodedFunctionCall = ERC20.encodeFunctionBalanceOf(address)
        val callParams = mapOf(
                "to" to contractAddress.toHexString(),
                "data" to encodedFunctionCall.toHexString())

        val request = Request("eth_call", listOf(callParams, "latest"))
        return service.makeRequestForBigInteger(request).flatMap {
            returnResultOrError(it)
        }
    }

    fun send(signedTransaction: ByteArray): Single<Unit> {
        val request = Request("eth_sendRawTransaction", listOf(signedTransaction.toHexString()))
        return service.makeRequestForString(request).map { Unit }
    }

    private fun <T> returnResultOrError(response: Response<T>): Single<T> {
        return if (response.error != null) {
            Single.error(Exception(response.error.message))
        } else {
            Single.just(response.result)
        }
    }

    class Request(val method: String, val params: List<Any> = listOf(), val id: Long = 1, val jsonrpc: String = "2.0")
    class Error(val code: Int, val message: String)
    class Response<T>(val result: T?, val error: Error?, val id: Long, val jsonrpc: String)

    private interface InfuraServiceAPI {
        @POST("/")
        fun makeRequestForBigInteger(@Body request: Request): Single<Response<BigInteger>>

        @POST("/")
        fun makeRequestForString(@Body request: Request): Single<Response<String>>

    }
}
