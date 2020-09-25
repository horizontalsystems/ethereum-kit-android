package io.horizontalsystems.ethereumkit.api

/*
import in3.Chain
import in3.IN3
import in3.Proof
import in3.eth1.Block.LATEST
import in3.eth1.LogFilter
import in3.eth1.TransactionRequest
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.reactivex.Single
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.logging.Logger

class IncubedRpcApiProvider(
        private val networkType: EthereumKit.NetworkType,
        private val address: Address
) : IRpcApiProvider {

    private val logger = Logger.getLogger("IncubedRpcApiProvider")
    private val GET_LOGS_REQUEST_MAX_BLOCKS_RANGE = 10000 // max blocks range for which eth_getLogs can be queried with no-proof, this limit is set by in3-c server

    private val in3: IN3 by lazy {
        when (networkType) {
            EthereumKit.NetworkType.MainNet -> Chain.MAINNET
            else -> Chain.KOVAN
        }.let { chain ->
            val in3Instance = IN3.forChain(chain)
            in3Instance.config.proof = Proof.none
            in3Instance
        }
    }
    private val eth1 by lazy { in3.eth1API }
    private val pool = Executors.newFixedThreadPool(1)

    @Synchronized
    private fun <T> serialExecute(block: () -> T): T {
        return pool.submit(block).get()
    }

    override val source: String
        get() = "Incubed"

    override fun getLastBlockHeight(): Single<Long> {
        logger.info("IncubedRpcApiProvider: getLastBlockHeight")
        return Single.fromCallable {
            serialExecute {
                eth1.blockNumber
            }
        }
    }

    override fun getTransactionCount(): Single<Long> {
        logger.info("IncubedRpcApiProvider: getTransactionCount")
        return Single.fromCallable {
            serialExecute {
                eth1.getTransactionCount(address.hex, LATEST).toLong()
            }
        }
    }

    override fun getBalance(): Single<BigInteger> {
        logger.info("IncubedRpcApiProvider: getBalance")
        return Single.fromCallable {
            serialExecute {
                eth1.getBalance(address.hex, LATEST)
            }
        }
    }

    override fun send(signedTransaction: ByteArray): Single<Unit> {
        logger.info("IncubedRpcApiProvider: send")
        return Single.fromCallable {
            serialExecute {
                eth1.sendRawTransaction(signedTransaction.toHexString())
            }
        }.map { Unit }

    }

    override fun getStorageAt(contractAddress: Address, position: String, blockNumber: Long?): Single<String> {
        logger.info("IncubedRpcApiProvider: getStorageAt")
        return Single.fromCallable {
            serialExecute {
                eth1.getStorageAt(contractAddress.hex, position.toBigInteger(), blockNumber
                        ?: LATEST)
            }
        }
    }

    override fun getLogs(address: Address?, fromBlock: Long, toBlock: Long, topics: List<ByteArray?>): Single<List<EthereumLog>> {
        logger.info("IncubedRpcApiProvider: getLogs")

        return Single.fromCallable {
            serialExecute {
                var requestFrom = fromBlock
                val logs = mutableListOf<EthereumLog>()
                while (requestFrom < toBlock) {

                    val requestTo = if (requestFrom + GET_LOGS_REQUEST_MAX_BLOCKS_RANGE > toBlock) toBlock else requestFrom + GET_LOGS_REQUEST_MAX_BLOCKS_RANGE
                    val partialLogs = getLogsBlocking(address, requestFrom, requestTo, topics)
                    logs.addAll(partialLogs)

                    requestFrom = requestTo + 1
                }
                logs
            }
        }
    }

    private fun getLogsBlocking(address: Address?, fromBlock: Long, toBlock: Long, topics: List<ByteArray?>): List<EthereumLog> {
        val logFilter = LogFilter().apply {
            this.address = address?.hex
            this.fromBlock = fromBlock
            this.toBlock = toBlock
            this.topics = Array(topics.size) { topics[it]?.toHexString() }
        }

        return eth1.getLogs(logFilter).map {
            EthereumLog(it.address, it.blockHash, it.blockNumber, "" */
/* TODO JNI Log class from in3-c doesn't have field 'data', need to add *//*
, it.logIndex, it.isRemoved, it.topics.toList(), it.transactionHash, it.gettTansactionIndex())
        }
    }

    override fun getBlock(blockNumber: Long): Single<Block> {
        logger.info("IncubedRpcApiProvider: getBlock")
        return Single.fromCallable {
            serialExecute {
                eth1.getBlockByNumber(blockNumber, false)
            }
        }.map {
            Block(it.number, it.timeStamp)
        }
    }

    override fun call(contractAddress: Address, data: ByteArray, blockNumber: Long?): Single<String> {
        logger.info("IncubedRpcApiProvider: call")
        return Single.fromCallable {
            serialExecute {
                val request = TransactionRequest().apply {
                    this.to = contractAddress.hex
                    this.data = data.toHexString()

                }
                val result = eth1.call(request, blockNumber ?: LATEST)
                result.toString()
            }
        }
    }

    override fun estimateGas(to: Address, value: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: String?): Single<Long> {
        logger.info("IncubedRpcApiProvider: estimateGas")
        return Single.fromCallable {
            serialExecute {
                val request = TransactionRequest().apply {
                    this.from = address.hex
                    this.to = to.hex
                    value?.let { this.value = it }
                    gasLimit?.let { this.gas = it }
                    gasPrice?.let { this.gasPrice = it }
                    data?.let { this.data = it }
                }
                eth1.estimateGas(request, LATEST)
            }
        }
    }

    override fun transactionReceiptStatus(transactionHash: ByteArray): Single<TransactionStatus> {
        logger.info("IncubedRpcApiProvider: transactionReceiptStatus: ${transactionHash.toHexString()}")
        return Single.fromCallable {
            serialExecute {
                val receipt = eth1.getTransactionReceipt(transactionHash.toHexString())
                logger.info("IncubedRpcApiProvider: transactionReceiptStatus receipt: ${receipt?.status}")
                when (receipt?.status) {
                    true -> TransactionStatus.SUCCESS
                    false -> TransactionStatus.FAILED
                    else -> TransactionStatus.NOTFOUND
                }
            }
        }
    }

    override fun transactionExist(transactionHash: ByteArray): Single<Boolean> {
        logger.info("IncubedRpcApiProvider: transactionExist")
        return Single.fromCallable {
            serialExecute {
                val tx = eth1.getTransactionByHash(transactionHash.toHexString())
                tx != null
            }
        }
    }

}
*/
