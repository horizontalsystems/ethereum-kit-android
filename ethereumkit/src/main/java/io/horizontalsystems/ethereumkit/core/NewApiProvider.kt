package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import java.math.BigInteger

class NewApiProvider : IApiProvider {

    override fun getGasPriceInWei(): Single<Long> {
        TODO("not implemented")
    }

    override fun getLastBlockHeight(): Single<Long> {
        TODO("not implemented")
    }

    override fun getTransactionCount(address: ByteArray): Single<Long> {
        TODO("not implemented")
    }

    override fun getBalance(address: ByteArray): Single<BigInteger> {
        TODO("not implemented")
    }

    override fun getBalanceErc20(address: ByteArray, contractAddress: ByteArray): Single<BigInteger> {
        TODO("not implemented")
    }

    override fun getTransactions(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        TODO("not implemented")
    }

    override fun getTransactionsErc20(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        TODO("not implemented")
    }

    override fun send(signedTransaction: ByteArray): Single<Unit> {
        TODO("not implemented")
    }
}
