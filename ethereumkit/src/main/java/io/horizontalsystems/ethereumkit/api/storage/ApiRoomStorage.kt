package io.horizontalsystems.ethereumkit.api.storage

import io.horizontalsystems.ethereumkit.api.models.EthereumBalance
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.core.IApiStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import java.math.BigInteger

class ApiRoomStorage(private val database: ApiDatabase) : IApiStorage {

    override fun getTransactions(fromHash: ByteArray?, limit: Int?, contractAddress: ByteArray?): Single<List<EthereumTransaction>> {
        val single =
                if (contractAddress == null)
                    database.transactionDao().getTransactions()
                else
                    database.transactionDao().getErc20Transactions(contractAddress)

        return single
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

    override fun getBalance(address: ByteArray): BigInteger? {
        return database.balanceDao().getBalance(address)?.balance
    }

    override fun getLastBlockHeight(): Long? {
        return database.lastBlockHeightDao().getLastBlockHeight()?.height?.toLong()
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

    override fun saveBalance(balance: BigInteger, address: ByteArray) {
        database.balanceDao().insert(EthereumBalance(address, balance))
    }

    override fun saveTransactions(ethereumTransactions: List<EthereumTransaction>) {
        database.transactionDao().insert(ethereumTransactions)
    }

}
