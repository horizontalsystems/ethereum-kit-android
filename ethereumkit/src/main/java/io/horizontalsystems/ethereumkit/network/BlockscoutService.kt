package io.horizontalsystems.ethereumkit.network

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.core.retryWhenError
import io.horizontalsystems.ethereumkit.network.EtherscanService.RequestError
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.util.logging.Logger

/**
 * Talks to a Blockscout instance's modern REST API (`/api/v2`). Unlike the legacy
 * Etherscan-compatible `/api` endpoint (see [EtherscanService]), the v2 endpoints are not
 * hard-throttled for anonymous callers.
 *
 * The v2 endpoints are cursor-paginated (newest first). Each response carries an opaque
 * `next_page_params` object that is echoed back as query params to fetch the next page. Since
 * results are ordered by descending block number, paging stops as soon as a page contains an
 * item older than [startBlock], or after [MAX_PAGES] pages as a safety bound on the initial sync.
 */
class BlockscoutService(
    baseUrl: String,
    private val apiKeys: List<String>,
) {
    private val logger = Logger.getLogger("BlockscoutService")
    private val service: BlockscoutServiceAPI

    // Non-throttled key raises limits when present; Blockscout ignores it otherwise.
    private val apiKey: String? = apiKeys.firstOrNull()?.takeIf { it.isNotBlank() }

    init {
        val loggingInterceptor = HttpLoggingInterceptor { logger.info(it) }
            .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mobile App Agent")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)

        val gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            // Async adapter (enqueue, not execute): TransactionSyncManager zips the per-address
            // syncers, and a synchronous adapter would block the single subscribe thread, running
            // the (slow) Blockscout requests one-after-another. Async lets them run concurrently,
            // so the first-sync wait is the slowest request, not the sum of all of them.
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        service = retrofit.create(BlockscoutServiceAPI::class.java)
    }

    fun getTransactions(address: String, startBlock: Long): Single<List<BlockscoutTransaction>> =
        fetchPages(startBlock, { it.blockNumber }) { params ->
            service.transactions(address, apiKey, params)
        }

    fun getInternalTransactions(address: String, startBlock: Long): Single<List<BlockscoutInternalTransaction>> =
        fetchPages(startBlock, { it.blockNumber }) { params ->
            service.internalTransactions(address, apiKey, params)
        }

    fun getInternalTransactions(txHash: String): Single<List<BlockscoutInternalTransaction>> =
        fetchPages(0, { it.blockNumber }) { params ->
            service.transactionInternalTransactions(txHash, apiKey, params)
        }

    fun getTokenTransfers(address: String, type: String, startBlock: Long): Single<List<BlockscoutTokenTransfer>> =
        fetchPages(startBlock, { it.blockNumber }) { params ->
            service.tokenTransfers(address, type, apiKey, params)
        }

    private fun <T> fetchPages(
        startBlock: Long,
        blockNumberOf: (T) -> Long?,
        call: (Map<String, String>) -> Single<Page<T>>,
    ): Single<List<T>> {
        val accumulated = mutableListOf<T>()

        fun nextPage(params: Map<String, String>, page: Int): Single<List<T>> =
            call(params).flatMap { response ->
                val items = response.items.orEmpty()
                var reachedOlder = false
                for (item in items) {
                    val blockNumber = blockNumberOf(item) ?: 0
                    if (blockNumber < startBlock) {
                        reachedOlder = true
                    } else {
                        accumulated.add(item)
                    }
                }

                val nextParams = response.nextPageParams?.toStringMap()
                if (reachedOlder || nextParams.isNullOrEmpty() || page + 1 >= MAX_PAGES) {
                    Single.just(accumulated.toList())
                } else {
                    nextPage(nextParams, page + 1)
                }
            }

        return nextPage(emptyMap(), 0).retryWhenError(RequestError.RateLimitExceed::class)
    }

    // next_page_params is a JSON object on non-final pages and JSON null on the last page. It must
    // be typed as JsonElement, not JsonObject: Gson's JsonObject adapter throws on a null value,
    // which would fail the whole page parse (and thus drop every single-page result).
    private fun JsonElement.toStringMap(): Map<String, String> {
        if (!isJsonObject) return emptyMap()
        val map = mutableMapOf<String, String>()
        for ((key, value) in asJsonObject.entrySet()) {
            if (value != null && !value.isJsonNull) {
                map[key] = value.asString
            }
        }
        return map
    }

    companion object {
        private const val MAX_PAGES = 20
    }

    private interface BlockscoutServiceAPI {
        @GET("api/v2/addresses/{address}/transactions")
        fun transactions(
            @Path("address") address: String,
            @Query("apikey") apiKey: String?,
            @QueryMap params: Map<String, String>,
        ): Single<Page<BlockscoutTransaction>>

        @GET("api/v2/addresses/{address}/internal-transactions")
        fun internalTransactions(
            @Path("address") address: String,
            @Query("apikey") apiKey: String?,
            @QueryMap params: Map<String, String>,
        ): Single<Page<BlockscoutInternalTransaction>>

        @GET("api/v2/transactions/{txHash}/internal-transactions")
        fun transactionInternalTransactions(
            @Path("txHash") txHash: String,
            @Query("apikey") apiKey: String?,
            @QueryMap params: Map<String, String>,
        ): Single<Page<BlockscoutInternalTransaction>>

        @GET("api/v2/addresses/{address}/token-transfers")
        fun tokenTransfers(
            @Path("address") address: String,
            @Query("type") type: String,
            @Query("apikey") apiKey: String?,
            @QueryMap params: Map<String, String>,
        ): Single<Page<BlockscoutTokenTransfer>>
    }
}

class Page<T>(
    @SerializedName("items") val items: List<T>?,
    @SerializedName("next_page_params") val nextPageParams: JsonElement?,
)

class BlockscoutAddress(
    @SerializedName("hash") val hash: String?,
)

class BlockscoutToken(
    @SerializedName("address_hash") val addressHash: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("decimals") val decimals: String?,
    @SerializedName("type") val type: String?,
)

class BlockscoutTotal(
    @SerializedName("value") val value: String?,
    @SerializedName("decimals") val decimals: String?,
    @SerializedName("token_id") val tokenId: String?,
)

class BlockscoutTransaction(
    @SerializedName("hash") val hash: String?,
    @SerializedName("block_number") val blockNumber: Long?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("nonce") val nonce: Long?,
    @SerializedName("position") val position: Int?,
    @SerializedName("from") val from: BlockscoutAddress?,
    @SerializedName("to") val to: BlockscoutAddress?,
    @SerializedName("value") val value: String?,
    @SerializedName("gas_limit") val gasLimit: String?,
    @SerializedName("gas_price") val gasPrice: String?,
    @SerializedName("gas_used") val gasUsed: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("raw_input") val rawInput: String?,
)

class BlockscoutInternalTransaction(
    @SerializedName("transaction_hash") val transactionHash: String?,
    @SerializedName("block_number") val blockNumber: Long?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("from") val from: BlockscoutAddress?,
    @SerializedName("to") val to: BlockscoutAddress?,
    @SerializedName("value") val value: String?,
    @SerializedName("index") val index: Int?,
)

class BlockscoutTokenTransfer(
    @SerializedName("transaction_hash") val transactionHash: String?,
    @SerializedName("block_number") val blockNumber: Long?,
    @SerializedName("block_hash") val blockHash: String?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("from") val from: BlockscoutAddress?,
    @SerializedName("to") val to: BlockscoutAddress?,
    @SerializedName("log_index") val logIndex: Int?,
    @SerializedName("token") val token: BlockscoutToken?,
    @SerializedName("total") val total: BlockscoutTotal?,
)
