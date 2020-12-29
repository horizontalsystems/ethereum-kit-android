package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.BalanceOfMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single
import java.math.BigInteger

class DataProvider(private val ethereumKit: EthereumKit) : IDataProvider {

    override val lastBlockHeight: Long
        get() = ethereumKit.lastBlockHeight ?: 0

    override fun getTransactionStatuses(transactionHashes: List<ByteArray>): Single<Map<ByteArray, TransactionStatus>> {

        val singles = transactionHashes.map { hash ->
            ethereumKit.transactionStatus(hash).flatMap { txStatus ->
                Single.just(Pair(hash, txStatus))
            }
        }

        return Single.zip(singles) { singleResults ->
            singleResults.map { it as? Pair<ByteArray, TransactionStatus> }
        }.flatMap { list ->
            val map = mutableMapOf<ByteArray, TransactionStatus>()
            list.forEach {
                if (it != null) {
                    map[it.first] = it.second
                }
            }
            Single.just(map)
        }
    }

    override fun getBalance(contractAddress: Address, address: Address): Single<BigInteger> {
        return ethereumKit.call(contractAddress, BalanceOfMethod(address).encodedABI())
                .map { it.toBigInteger() }
    }

    override fun send(contractAddress: Address, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray> {
        return ethereumKit.send(contractAddress, BigInteger.ZERO, transactionInput, gasPrice, gasLimit).map { tx ->
            tx.hash
        }
    }
}