package io.horizontalsystems.ethereumkit.api.storage

import io.horizontalsystems.ethereumkit.api.models.EthereumBalance
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.core.IApiStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import java.math.BigInteger

class ApiRoomStorage(private val database: ApiDatabase) : IApiStorage {

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return database.transactionDao().getTransactions()
                .flatMap { transactionsList ->
                    var transactions = transactionsList

                    fromHash?.let { fromHash ->
                        val tx = transactions.firstOrNull { it.hash.contentEquals(fromHash) }
                        tx?.timestamp?.let { txTimeStamp ->
                            transactions = transactions.filter { it.timestamp < txTimeStamp }
                        }
                    }

                    limit?.let {
                        transactions = transactions.take(it)
                    }

                    Single.just(transactions)
                }
    }

    override fun getBalance(): BigInteger? {
        return database.balanceDao().getBalance()?.balance
    }

    override fun getLastBlockHeight(): Long? {
        return database.lastBlockHeightDao().getLastBlockHeight()?.height
    }

    override fun getLastTransactionBlockHeight(isErc20: Boolean): Long? {
        return if (isErc20) {
            database.transactionDao().getTokenLastTransaction()?.blockNumber
        } else {
            database.transactionDao().getLastTransaction()?.blockNumber
        }
    }

    override fun saveLastBlockHeight(lastBlockHeight: Long) {
        database.lastBlockHeightDao().insert(LastBlockHeight(lastBlockHeight))
    }

    override fun saveBalance(balance: BigInteger) {
        database.balanceDao().insert(EthereumBalance(balance))
    }

    override fun saveTransactions(ethereumTransactions: List<EthereumTransaction>) {
        database.transactionDao().insert(ethereumTransactions)
    }

}
