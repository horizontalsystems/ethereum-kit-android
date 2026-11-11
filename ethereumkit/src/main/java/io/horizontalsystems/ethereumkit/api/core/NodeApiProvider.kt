package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class NodeApiProvider(
    private val uris: List<URI>,
    private val gson: Gson,
    auth: String? = null
) : IRpcApiProvider {

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val service: InfuraService
    private var currentRpcId = AtomicInteger(0)
    private val failover = UriFailover(uris)

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val headersInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            auth?.let {
                requestBuilder.header("Authorization", Credentials.basic("", auth))
            }
            chain.proceed(requestBuilder.build())
        }

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(headersInterceptor)

        val retrofit = Retrofit.Builder()
                .baseUrl("${uris.first()}/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraService::class.java)
    }

    override val source: String = uris.first().host

    override suspend fun <T: Any> execute(rpc: JsonRpc<T>): T {
        rpc.id = currentRpcId.addAndGet(1)
        val json = gson.toJson(rpc)

        return failover.execute { uri ->
            rpc.parseResponse(service.execute(uri, json), gson)
        }
    }

    private interface InfuraService {
        @POST
        @Headers("Content-Type: application/json", "Accept: application/json")
        suspend fun execute(@Url uri: URI, @Body jsonRpc: String): RpcResponse
    }

    sealed class ApiProviderError : Throwable() {
        object ApiUrlNotFound : ApiProviderError()
    }

}
