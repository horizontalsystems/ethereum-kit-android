package io.horizontalsystems.ethereumkit.network

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Maybe
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.web3j.utils.Convert
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

class ApiGasPrice {

    private val baseUrl = "https://ipfs.io"

    private val service: GasPriceServiceAPI

    init {

        val logger = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
                .connectTimeout(40, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
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

        service = retrofit.create(GasPriceServiceAPI::class.java)
    }

    fun getGasPrice(): Maybe<GasPrice> {
        return service.getEstimatedFeeList()
                .flatMap {
                    val feeRate = it.rates["ETH"]
                    if (feeRate == null) {
                        Maybe.empty()
                    } else {
                        Maybe.just(GasPrice(getInWei(feeRate.lowInGwei), getInWei(feeRate.mediumInGwei), getInWei(feeRate.highInGwei), it.time))
                    }
                }
    }

    private fun getInWei(gasInGwei: Long): Long {
        val gasInWei = Convert.toWei(gasInGwei.toBigDecimal(), Convert.Unit.GWEI)
        return gasInWei.toLong()
    }


    interface GasPriceServiceAPI{
        @GET("/ipns/QmSFtDtg9vbtrEdE1r31nToT3LEcWa7So4ZHNjFPatwK2S/blockchain/estimatefee/index.json")
        fun getEstimatedFeeList(): Maybe<EstimatedFees>
    }

    data class EstimatedFees(val rates: Map<String, FeeRate>, val time: Long)

    data class FeeRate(
            @SerializedName("low_priority") val lowInGwei: Long,
            @SerializedName("medium_priority") val mediumInGwei: Long,
            @SerializedName("high_priority") val highInGwei: Long
    )
}
