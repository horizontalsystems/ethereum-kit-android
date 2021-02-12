package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.IRpcApiProvider
import io.reactivex.Single
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.logging.Logger

class InfuraRpcApiProvider(
        private val url: String,
        private val gson: Gson,
        projectSecret: String? = null
) : IRpcApiProvider {

    private val logger = Logger.getLogger("InfuraService")
    private val service: InfuraService

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY)

        val headersInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            projectSecret?.let { projectSecret ->
                requestBuilder.header("Authorization", Credentials.basic("", projectSecret))
            }
            chain.proceed(requestBuilder.build())
        }

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(headersInterceptor)

        val retrofit = Retrofit.Builder()
                .baseUrl("$url/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraService::class.java)
    }

    override val source = "Infura"

    override fun <T> single(rpc: JsonRpc<T>): Single<T> {
        return service.single(url, gson.toJson(rpc))
                .map { response ->
                    rpc.parseResponse(response, gson)
                }
    }

    private interface InfuraService {
        @POST
        @Headers("Content-Type: application/json", "Accept: application/json")
        fun single(@Url url: String, @Body jsonRpc: String): Single<RpcResponse>
    }

}
