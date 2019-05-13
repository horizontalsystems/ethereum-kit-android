package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.InfuraCredentials
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.network.InfuraService
import io.reactivex.Single
import java.math.BigInteger

class RpcApiProvider(private val infuraService: InfuraService) : IRpcApiProvider {

    override fun getLastBlockHeight(): Single<Long> {
        return infuraService.getLastBlockHeight()
    }

    override fun getTransactionCount(address: ByteArray): Single<Long> {
        return infuraService.getTransactionCount(address)
    }

    override fun getBalance(address: ByteArray): Single<BigInteger> {
        return infuraService.getBalance(address)
    }

    override fun send(signedTransaction: ByteArray): Single<Unit> {
        return infuraService.send(signedTransaction)
    }

    override fun getStorageAt(contractAddress: ByteArray, position: String, blockNumber: Long?): Single<String> {
        return infuraService.getStorageAt(contractAddress, position, blockNumber)
    }

    override fun getLogs(address: ByteArray?, fromBlock: Long?, toBlock: Long?, topics: List<ByteArray?>): Single<List<EthereumLog>> {
        return infuraService.getLogs(address, fromBlock, toBlock, topics)
    }

    override fun getBlock(blockNumber: Long): Single<Block> {
        return infuraService.getBlockByNumber(blockNumber)
    }

    override fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<String> {
        return infuraService.call(contractAddress, data, blockNumber)
    }

    companion object {
        fun getInstance(networkType: NetworkType, infuraCredentials: InfuraCredentials): RpcApiProvider {
            val infuraService = InfuraService(networkType, infuraCredentials)

            return RpcApiProvider(infuraService)
        }
    }

}
