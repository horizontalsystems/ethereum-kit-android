package io.horizontalsystems.ethereumkit.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.models.etherscan.*
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.logging.Logger

class EtherscanService(private val networkType: NetworkType,
                       private val apiKey: String) {

    private val logger = Logger.getLogger("EtherscanService")

    private val service: EtherscanServiceAPI

    private val baseUrl: String
        get() {
            return when (networkType) {
                NetworkType.MainNet -> "https://api.etherscan.io"
                NetworkType.Ropsten -> "https://api-ropsten.etherscan.io"
                NetworkType.Kovan -> "https://api-kovan.etherscan.io"
                NetworkType.Rinkeby -> "https://api-rinkeby.etherscan.io"
            }
        }

    private val gson: Gson

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)

        gson = GsonBuilder()
                .setLenient()
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(EtherscanServiceAPI::class.java)
    }

    fun getTransactionList(address: ByteArray, startBlock: Long): Single<EtherscanResponse> {
        return service.getTransactionList("account", "txList", address.toHexString(), startBlock, 99_999_999, "desc", apiKey)
                .map { parseResponse(it) }
                .retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getInternalTransactionList(address: ByteArray, startBlock: Long): Single<EtherscanResponse> {
        return service.getTransactionList("account", "txlistinternal", address.toHexString(), startBlock, 99_999_999, "desc", apiKey)
                .map { parseResponse(it) }
                .retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getTokenTransactions(contractAddress: ByteArray, address: ByteArray, startBlock: Long): Single<EtherscanResponse> {
        return service.getTokenTransactions("account", "tokentx", contractAddress.toHexString(), address.toHexString(), startBlock, 99_999_999, "desc", apiKey)
                .map { parseResponse(it) }
                .retryWhenError(RequestError.RateLimitExceed::class)
    }

    private fun parseResponse(response: JsonElement): EtherscanResponse {
        try {
            val responseObj = response.asJsonObject
            val status = responseObj["status"].asJsonPrimitive.asString
            val message = responseObj["message"].asJsonPrimitive.asString

            if (status == "0" && message != "No transactions found") {
                val result = responseObj["result"].asJsonPrimitive.asString
                if (message == "NOTOK" && result == "Max rate limit reached") {
                    throw RequestError.RateLimitExceed()
                }
            }
            val result: List<Map<String, String>> = gson.fromJson(responseObj["result"], object : TypeToken<List<Map<String, String>>>() {}.type)
            return EtherscanResponse(status, message, result)

        } catch (rateLimitExceeded: RequestError.RateLimitExceed) {
            throw rateLimitExceeded
        } catch (err: Throwable) {
            throw RequestError.ResponseError("Unexpected response: $response")
        }
    }

    open class RequestError(message: String? = null) : Exception(message ?: "") {
        class ResponseError(message: String) : RequestError(message)
        class RateLimitExceed : RequestError()
    }

    private interface EtherscanServiceAPI {

        @GET("/api")
        fun getTransactionList(
                @Query("module") module: String,
                @Query("action") action: String,
                @Query("address") address: String,
                @Query("startblock") startblock: Long,
                @Query("endblock") endblock: Long,
                @Query("sort") sort: String,
                @Query("apiKey") apiKey: String): Single<JsonElement>

        @GET("/api")
        fun getTokenTransactions(
                @Query("module") module: String,
                @Query("action") action: String,
                @Query("contractaddress") contractAddress: String,
                @Query("address") address: String,
                @Query("startblock") startblock: Long,
                @Query("endblock") endblock: Long,
                @Query("sort") sort: String,
                @Query("apiKey") apiKey: String): Single<JsonElement>
    }

}
