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
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.logging.Logger

class InfuraRpcApiProvider(
        domain: String,
        private val projectId: String,
        projectSecret: String?,
        private val gson: Gson
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
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(headersInterceptor)

        val retrofit = Retrofit.Builder()
                .baseUrl("https://$domain/v3/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraService::class.java)
    }

    override val source = "Infura"

    override fun <T> single(rpc: JsonRpc<T>): Single<T> {
        return service.single(projectId, gson.toJson(rpc))
                .map { response ->
                    rpc.parseResponse(response, gson)
                }
    }

    private interface InfuraService {
        @POST("{projectId}")
        fun single(@Path("projectId") projectId: String,
                   @Body jsonRpc: String): Single<RpcResponse>
    }

}
