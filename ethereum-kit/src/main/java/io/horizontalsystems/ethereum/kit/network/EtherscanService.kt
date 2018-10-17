package io.horizontalsystems.ethereum.kit.network

import com.google.gson.GsonBuilder
import io.horizontalsystems.ethereum.kit.models.etherscan.EtherscanResponse
import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class EtherscanService(networkType: NetworkType) {

    private val service: EtherscanServiceAPI
    private val apiKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"

    init {
        val baseUrl = when (networkType) {
            NetworkType.MainNet -> "https://api.etherscan.io"
            NetworkType.Ropsten -> "https://api-ropsten.etherscan.io"
            NetworkType.Kovan -> "https://api-kovan.etherscan.io"
            NetworkType.Rinkeby -> "https://api-rinkeby.etherscan.io"
        }

        val logger = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BASIC)

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

    fun getTransactionList(address: String): Observable<EtherscanResponse> =
            service.getTransactionList("account", "txList", address, 0, 99_999_999, "desc", apiKey)


    interface EtherscanServiceAPI {

        @GET("/api")
        fun getTransactionList(
                @Query("module") module: String,
                @Query("action") action: String,
                @Query("address") address: String,
                @Query("startblock") startblock: Int,
                @Query("endblock") endblock: Int,
                @Query("sort") sort: String,
                @Query("apiKey") apiKey: String): Observable<EtherscanResponse>
    }

}
