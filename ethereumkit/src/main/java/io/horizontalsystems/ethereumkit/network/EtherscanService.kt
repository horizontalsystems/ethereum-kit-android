package io.horizontalsystems.ethereumkit.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.models.EtherscanResponse
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class EtherscanService(
    baseUrl: String,
    private val apiKeys: List<String>,
    private val chainId: Int,
) {
    private val apiKeyIndex = AtomicInteger(0)

    private val logger = Logger.getLogger("EtherscanService")

    private val service: EtherscanServiceAPI

    private val gson: Gson

    init {
        val loggingInterceptor = HttpLoggingInterceptor {
            logger.info(it)
        }.setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url

                val url = originalUrl.newBuilder()
                    .addQueryParameter("apikey", getNextApiKey())
                    .addQueryParameter("chainid", chainId.toString())
                    .build()

                val request = originalRequest.newBuilder()
                    .header("User-Agent", "Mobile App Agent")
                    .url(url)
                    .build()

                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)

        gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        service = retrofit.create(EtherscanServiceAPI::class.java)
    }

    private fun getNextApiKey(): String {
        val index = apiKeyIndex.getAndUpdate { (it + 1) % apiKeys.size }
        return apiKeys[index]
    }

    suspend fun getTransactionList(address: Address, startBlock: Long): EtherscanResponse {
        return retryWhenError(RequestError.RateLimitExceed::class) {
            val response = service.accountApi(
                action = "txlist",
                address = address.hex,
                startBlock = startBlock,
            )
            parseResponse(response)
        }
    }

    suspend fun getInternalTransactionList(address: Address, startBlock: Long): EtherscanResponse {
        return retryWhenError(RequestError.RateLimitExceed::class) {
            val response = service.accountApi(
                action = "txlistinternal",
                address = address.hex,
                startBlock = startBlock,
            )
            parseResponse(response)
        }
    }

    suspend fun getTokenTransactions(address: Address, startBlock: Long): EtherscanResponse {
        return retryWhenError(RequestError.RateLimitExceed::class) {
            val response = service.accountApi(
                action = "tokentx",
                address = address.hex,
                startBlock = startBlock,
            )
            parseResponse(response)
        }
    }

    suspend fun getInternalTransactionsAsync(transactionHash: ByteArray): EtherscanResponse {
        return retryWhenError(RequestError.RateLimitExceed::class) {
            val response = service.accountApi(
                action = "txlistinternal",
                txHash = transactionHash.toHexString(),
            )
            parseResponse(response)
        }
    }

    suspend fun getEip721Transactions(address: Address, startBlock: Long): EtherscanResponse {
        return retryWhenError(RequestError.RateLimitExceed::class) {
            val response = service.accountApi(
                action = "tokennfttx",
                address = address.hex,
                startBlock = startBlock,
            )
            parseResponse(response)
        }
    }

    suspend fun getEip1155Transactions(address: Address, startBlock: Long): EtherscanResponse {
        return retryWhenError(RequestError.RateLimitExceed::class) {
            val response = service.accountApi(
                action = "token1155tx",
                address = address.hex,
                startBlock = startBlock,
            )
            parseResponse(response)
        }
    }

    private fun parseResponse(response: JsonElement): EtherscanResponse {
        try {
            val responseObj = response.asJsonObject
            val status = responseObj["status"].asJsonPrimitive.asString
            val message = responseObj["message"].asJsonPrimitive.asString

            if (status == "0" && message != "No transactions found") {
                val resultElement = responseObj["result"]
                val result = if (resultElement != null && resultElement.isJsonPrimitive) resultElement.asString else null

                // Etherscan signals throttling with message "NOTOK" / result "Max rate limit reached";
                // Blockscout's legacy endpoint uses "Too many requests" with a null result. Surface both
                // as RateLimitExceed so retryWhenError backs off instead of failing the whole sync.
                val rateLimited = (message == "NOTOK" && result == "Max rate limit reached") ||
                        message.contains("Too many requests", ignoreCase = true)
                if (rateLimited) {
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
        @GET("api")
        suspend fun accountApi(
            @Query("module") module: String = "account",
            @Query("action") action: String,
            @Query("address") address: String? = null,
            @Query("txhash") txHash: String? = null,
            @Query("startblock") startBlock: Long? = null,
            @Query("endblock") endBlock: Long? = null,
            @Query("sort") sort: String? = "desc"
        ): JsonElement
    }

}
