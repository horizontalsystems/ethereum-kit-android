package io.horizontalsystems.ethereumkit.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.models.EtherscanResponse
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.logging.Logger

class EtherscanService(
    baseUrl: String,
    private val apiKey: String
) {

    private val logger = Logger.getLogger("EtherscanService")

    private val service: EtherscanServiceAPI

    private val gson: Gson

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(Interceptor { chain ->
                val originalRequest: Request = chain.request()
                val requestWithUserAgent: Request = originalRequest.newBuilder()
                    .header("User-Agent", "Mobile App Agent")
                    .build()
                chain.proceed(requestWithUserAgent)
            })

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

    fun getTransactionList(address: Address, startBlock: Long): Single<EtherscanResponse> {
        return service.accountApi(
            action = "txList",
            address = address.hex,
            startBlock = startBlock,
            apiKey = apiKey
        ).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getInternalTransactionList(address: Address, startBlock: Long): Single<EtherscanResponse> {
        return service.accountApi(
            action = "txlistinternal",
            address = address.hex,
            startBlock = startBlock,
            apiKey = apiKey
        ).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getTokenTransactions(address: Address, startBlock: Long): Single<EtherscanResponse> {
        return service.accountApi(
            action = "tokentx",
            address = address.hex,
            startBlock = startBlock,
            apiKey = apiKey
        ).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getInternalTransactionsAsync(transactionHash: ByteArray): Single<EtherscanResponse> {
        return service.accountApi(
            action = "txlistinternal",
            txHash = transactionHash.toHexString(),
            apiKey = apiKey
        ).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getEip721Transactions(address: Address, startBlock: Long): Single<EtherscanResponse> {
        return service.accountApi(
            action = "tokennfttx",
            address = address.hex,
            startBlock = startBlock,
            apiKey = apiKey
        ).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
    }

    fun getEip1155Transactions(address: Address, startBlock: Long): Single<EtherscanResponse> {
        return service.accountApi(
            action = "token1155tx",
            address = address.hex,
            startBlock = startBlock,
            apiKey = apiKey
        ).map {
            parseResponse(it)
        }.retryWhenError(RequestError.RateLimitExceed::class)
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
        fun accountApi(
            @Query("module") module: String = "account",
            @Query("action") action: String,
            @Query("address") address: String? = null,
            @Query("txhash") txHash: String? = null,
            @Query("startblock") startBlock: Long? = null,
            @Query("endblock") endBlock: Long? = 99_999_999,
            @Query("sort") sort: String? = "desc",
            @Query("apikey") apiKey: String
        ): Single<JsonElement>
    }

}
