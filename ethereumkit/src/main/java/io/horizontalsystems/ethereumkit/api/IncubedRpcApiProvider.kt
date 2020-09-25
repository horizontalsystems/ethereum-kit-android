package io.horizontalsystems.ethereumkit.api

import com.google.gson.Gson
import com.google.gson.JsonParser
import in3.Chain
import in3.IN3
import in3.Proof
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.IRpcApiProvider
import io.reactivex.Single
import java.util.concurrent.Executors
import java.util.logging.Logger

class IncubedRpcApiProvider(
        private val networkType: NetworkType,
        private val gson: Gson
) : IRpcApiProvider {

    private val logger = Logger.getLogger("IncubedRpcApiProvider")

    private val in3: IN3 by lazy {
        when (networkType) {
            NetworkType.MainNet -> Chain.MAINNET
            else -> Chain.KOVAN
        }.let { chain ->
            val in3Instance = IN3.forChain(chain)
            in3Instance.config.proof = Proof.none
            in3Instance
        }
    }
    private val pool = Executors.newFixedThreadPool(1)

    @Synchronized
    private fun <T> serialExecute(block: () -> T): T {
        return pool.submit(block).get()
    }

    override val source = "Incubed"

    override fun <T> single(rpc: JsonRpc<T>): Single<T> {
        return Single.fromCallable {
            serialExecute {
                val request = gson.toJson(rpc)
                logger.info("request: $request")

                val resultString = in3.send(request)
                logger.info("result: $resultString")

                val result = JsonParser.parseString(resultString)
                rpc.parseResult(result, gson)
            }
        }
    }
}
