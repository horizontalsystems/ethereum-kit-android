package io.horizontalsystems.ethereumkit.core

import in3.Chain
import in3.IN3
import in3.Proof
import in3.eth1.Block.LATEST
import in3.eth1.LogFilter
import in3.eth1.TransactionRequest
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.reactivex.Single
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.logging.Logger

class IncubedRpcApiProvider(
        private val networkType: EthereumKit.NetworkType,
        private val address: ByteArray
) : IRpcApiProvider {

    private val logger = Logger.getLogger("IncubedRpcApiProvider")
    private val GET_LOGS_REQUEST_MAX_BLOCKS_RANGE = 10000 // max blocks range for which eth_getLogs can be queried with no-proof, this limit is set by in3-c server

    private val in3: IN3 by lazy {
        when (networkType) {
            EthereumKit.NetworkType.MainNet -> Chain.MAINNET
            else -> Chain.KOVAN
        }.let { chain ->
            val in3Instance = IN3.forChain(chain)
            in3Instance.proof = Proof.none
            in3Instance
        }
    }
    private val eth1 by lazy { in3.eth1API }
    private val pool = Executors.newFixedThreadPool(1)

    @Synchronized
    private fun <T> execute(block: () -> T): T {
        return pool.submit(block).get()

//        return Single.fromCallable {
//            block.invoke()
//        }.unsubscribeOn(scheduler)
//                .subscribeOn(scheduler)
//                .observeOn(scheduler)
//                .blockingGet()
    }

    override val source: String
        get() = "Incubed"

    override fun getLastBlockHeight(): Single<Long> {
        logger.info("IncubedRpcApiProvider: getLastBlockHeight")
        return Single.fromCallable {
            execute { eth1.blockNumber }
        }
    }

    override fun getTransactionCount(): Single<Long> {
        logger.info("IncubedRpcApiProvider: getTransactionCount")
        return Single.fromCallable { eth1.getTransactionCount(address.toHexString(), LATEST).toLong() }
    }

    override fun getBalance(): Single<BigInteger> {
        logger.info("IncubedRpcApiProvider: getBalance")
        return Single.fromCallable {
            execute { eth1.getBalance(address.toHexString(), LATEST) }
        }
    }

    override fun send(signedTransaction: ByteArray): Single<Unit> {
        logger.info("IncubedRpcApiProvider: send")
        return Single.fromCallable { eth1.sendRawTransaction(signedTransaction.toHexString()) }.map { Unit }
    }

    override fun getStorageAt(contractAddress: ByteArray, position: String, blockNumber: Long?): Single<String> {
        logger.info("IncubedRpcApiProvider: getStorageAt")
        return Single.fromCallable {
            eth1.getStorageAt(contractAddress.toHexString(), position.toBigInteger(), blockNumber
                    ?: LATEST)
        }
    }

    override fun getLogs(address: ByteArray?, fromBlock: Long, toBlock: Long, topics: List<ByteArray?>): Single<List<EthereumLog>> {
        logger.info("IncubedRpcApiProvider: getLogs")

        in3.proof = Proof.none  // TODO this assignment doesn't have any effect, need to debug in in3-c client code

        return Single.fromCallable {
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

    private fun getLogsBlocking(address: ByteArray?, fromBlock: Long, toBlock: Long, topics: List<ByteArray?>): List<EthereumLog> {
        val logFilter = LogFilter().apply {
            this.address = address?.toHexString()
            this.fromBlock = fromBlock
            this.toBlock = toBlock
            this.topics = Array(topics.size) { topics[it]?.toHexString() }
        }

        return eth1.getLogs(logFilter).map {
            EthereumLog(it.address, it.blockHash, it.blockNumber, "" /* TODO JNI Log class from in3-c doesn't have field 'data', need to add */, it.logIndex, it.isRemoved, it.topics.toList(), it.transactionHash, it.gettTansactionIndex())
        }
    }

    override fun getBlock(blockNumber: Long): Single<Block> {
        logger.info("IncubedRpcApiProvider: getBlock")
        return Single.fromCallable { eth1.getBlockByNumber(blockNumber, false) }.map {
            Block(it.number, it.timeStamp)
        }
    }

    override fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<String> {
        logger.info("IncubedRpcApiProvider: call")
        return Single.fromCallable {
            execute {
                val request = TransactionRequest().apply {
                    this.to = contractAddress.toHexString()
                    this.data = data.toHexString()

                }
                eth1.call(request, blockNumber ?: LATEST).toString()
            }
        }
    }

    override fun estimateGas(from: String?, to: String, value: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: String?): Single<String> {
        logger.info("IncubedRpcApiProvider: estimateGas")
        return Single.fromCallable {
            val request = TransactionRequest().apply {
                from?.let { this.from = it }
                this.to = to
                value?.let { this.value = it }
                gasLimit?.let { this.gas = it }
                gasPrice?.let { this.gasPrice = it }
                data?.let { this.data = it }
            }
            eth1.estimateGas(request, LATEST).toString()
        }
    }

    override fun transactionReceiptStatus(transactionHash: ByteArray): Single<TransactionStatus> {
        logger.info("IncubedRpcApiProvider: transactionReceiptStatus")
        return Single.fromCallable {
            val receipt = eth1.getTransactionReceipt(transactionHash.toHexString())
            if (receipt.status) {
                TransactionStatus.FAILED
            } else {
                TransactionStatus.SUCCESS
            }
        }
    }

    override fun transactionExist(transactionHash: ByteArray): Single<Boolean> {
        logger.info("IncubedRpcApiProvider: transactionExist")
        return Single.fromCallable {
            val tx = eth1.getTransactionByHash(transactionHash.toHexString())
            tx != null
        }
    }

}
