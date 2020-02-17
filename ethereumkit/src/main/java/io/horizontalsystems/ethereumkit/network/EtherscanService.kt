package io.horizontalsystems.ethereumkit.network

import com.google.gson.GsonBuilder
import io.horizontalsystems.ethereumkit.api.models.etherscan.EtherscanResponse
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
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

    init {
        val logger = HttpLoggingInterceptor {
            logger.info(it)
        }.setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(logger)

        val gson = GsonBuilder()
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
    }

    fun getTokenTransactions(contractAddress: ByteArray, address: ByteArray, startBlock: Long): Single<EtherscanResponse> {
        return service.getTokenTransactions("account", "tokentx", contractAddress.toHexString(), address.toHexString(), startBlock, 99_999_999, "desc", apiKey)
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
                @Query("apiKey") apiKey: String): Single<EtherscanResponse>

        @GET("/api")
        fun getTokenTransactions(
                @Query("module") module: String,
                @Query("action") action: String,
                @Query("contractaddress") contractAddress: String,
                @Query("address") address: String,
                @Query("startblock") startblock: Long,
                @Query("endblock") endblock: Long,
                @Query("sort") sort: String,
                @Query("apiKey") apiKey: String): Single<EtherscanResponse>
    }

}
