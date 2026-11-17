package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.models.RpcSource
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class NodeApiProvider(
    private val endpoints: List<RpcSource.Endpoint>,
    private val gson: Gson,
) : IRpcApiProvider {

    constructor(uris: List<URI>, gson: Gson, auth: String? = null) : this(uris.map { RpcSource.Endpoint(it, auth) }, gson)

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val service: InfuraService
    private var currentRpcId = AtomicInteger(0)
    private val failover = UriFailover(endpoints)

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
                .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)

        val retrofit = Retrofit.Builder()
                .baseUrl("${endpoints.first().uri}/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraService::class.java)
    }

    override val source: String = endpoints.first().uri.host

    override suspend fun <T: Any> execute(rpc: JsonRpc<T>): T {
        rpc.id = currentRpcId.addAndGet(1)
        val json = gson.toJson(rpc)

        return failover.execute { endpoint ->
            rpc.parseResponse(service.execute(endpoint.uri, authorization(endpoint.auth), json), gson)
        }
    }

    private interface InfuraService {
        @POST
        @Headers("Content-Type: application/json", "Accept: application/json")
        suspend fun execute(
            @Url uri: URI,
            // Left out of the request when null
            @Header("Authorization") authorization: String?,
            @Body jsonRpc: String,
        ): RpcResponse
    }

    companion object {
        // The secret belongs to one endpoint, so the header is built for each request and
        // not set on the shared client.
        internal fun authorization(auth: String?): String? = auth?.let { Credentials.basic("", it) }
    }

    sealed class ApiProviderError : Throwable() {
        object ApiUrlNotFound : ApiProviderError()
    }

}
