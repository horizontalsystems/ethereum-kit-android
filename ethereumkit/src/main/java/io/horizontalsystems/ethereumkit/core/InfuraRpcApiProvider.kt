package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.InfuraCredentials
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.horizontalsystems.ethereumkit.network.InfuraService
import io.reactivex.Single
import java.math.BigInteger

class InfuraRpcApiProvider(
        private val infuraService: InfuraService,
        private val address: ByteArray
) : IRpcApiProvider {

    override val source: String
        get() = "infura.io"

    override fun getLastBlockHeight(): Single<Long> {
        return infuraService.getLastBlockHeight()
    }

    override fun getTransactionCount(): Single<Long> {
        return infuraService.getTransactionCount(address)
    }

    override fun getBalance(): Single<BigInteger> {
        return infuraService.getBalance(address)
    }

    override fun send(signedTransaction: ByteArray): Single<Unit> {
        return infuraService.send(signedTransaction)
    }

    override fun estimateGas(to: String, value: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: String?): Single<Long> {
        return infuraService.estimateGas(address.toHexString(), to, value, gasLimit, gasPrice, data)
                .flatMap {
                    Single.just(BigInteger(it.replace("0x", ""), 16).toLong())
                }
    }

    override fun transactionReceiptStatus(transactionHash: ByteArray): Single<TransactionStatus> {
        return infuraService.transactionReceiptStatus(transactionHash)
    }

    override fun transactionExist(transactionHash: ByteArray): Single<Boolean> {
        return infuraService.transactionExist(transactionHash)
    }

    override fun getStorageAt(contractAddress: ByteArray, position: String, blockNumber: Long?): Single<String> {
        return infuraService.getStorageAt(contractAddress, position, blockNumber)
    }

    override fun getLogs(address: ByteArray?, fromBlock: Long, toBlock: Long, topics: List<ByteArray?>): Single<List<EthereumLog>> {
        return infuraService.getLogs(address, fromBlock, toBlock, topics)
    }

    override fun getBlock(blockNumber: Long): Single<Block> {
        return infuraService.getBlockByNumber(blockNumber)
    }

    override fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<String> {
        return infuraService.call(contractAddress, data, blockNumber)
    }

    companion object {
        fun getInstance(networkType: NetworkType, infuraCredentials: InfuraCredentials, address: ByteArray): InfuraRpcApiProvider {
            val infuraService = InfuraService(networkType, infuraCredentials)

            return InfuraRpcApiProvider(infuraService, address)
        }
    }

}
