package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
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
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraService::class.java)
    }

    override val source: String = uris.first().host

    override fun <T> single(rpc: JsonRpc<T>): Single<T> {
        rpc.id = currentRpcId.addAndGet(1)

        return Single.create { emitter ->
            var error: Throwable = ApiProviderError.ApiUrlNotFound

            for (uri in uris) {
                try {
                    val rpcResponse = service.single(uri, gson.toJson(rpc)).blockingGet()
                    val response = rpc.parseResponse(rpcResponse, gson)

                    emitter.onSuccess(response)
                    return@create
                } catch (throwable: Throwable) {
                    error = throwable
                    if (throwable is JsonRpc.ResponseError.RpcError) {
                        break
                    }
                }
            }
            emitter.onError(error)
        }
    }

    private interface InfuraService {
        @POST
        @Headers("Content-Type: application/json", "Accept: application/json")
        fun single(@Url uri: URI, @Body jsonRpc: String): Single<RpcResponse>
    }

    sealed class ApiProviderError : Throwable() {
        object ApiUrlNotFound : ApiProviderError()
    }

}
