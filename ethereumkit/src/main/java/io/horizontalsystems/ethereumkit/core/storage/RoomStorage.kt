package io.horizontalsystems.ethereumkit.core.storage

import android.content.Context
import io.horizontalsystems.ethereumkit.core.IStorage
import io.horizontalsystems.ethereumkit.models.EthereumBalance
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.LastBlockHeight
import io.reactivex.Single

class RoomStorage(databaseName: String, context: Context) : IStorage {

    private val database: KitDatabase = KitDatabase.getInstance(context, databaseName)


    override fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>> {
        val single =
                if (contractAddress == null)
                    database.transactionDao().getTransactions()
                else
                    database.transactionDao().getErc20Transactions(contractAddress)

        return single
                .flatMap { transactionsList ->
                    var transactions = transactionsList

                    fromHash?.let { fromHash ->
                        val tx = transactions.firstOrNull { it.hash == fromHash }
                        tx?.timeStamp?.let { txTimeStamp ->
                            transactions = transactions.filter { it.timeStamp < txTimeStamp }
                        }
                    }

                    limit?.let {
                        transactions = transactions.take(it)
                    }

                    Single.just(transactions)
                }
    }

    override fun getBalance(address: String): String? {
        return database.balanceDao().getBalance(address)?.balance
    }

    override fun getLastBlockHeight(): Int? {
        return database.lastBlockHeightDao().getLastBlockHeight()?.height
    }

    override fun getLastTransactionBlockHeight(isErc20: Boolean): Int? {
        if (isErc20) {
            return database.transactionDao().getTokenLastTransaction()?.blockNumber?.toInt()
        } else {
            return database.transactionDao().getLastTransaction()?.blockNumber?.toInt()
        }
    }

    override fun saveLastBlockHeight(lastBlockHeight: Int) {
        database.lastBlockHeightDao().insert(LastBlockHeight(lastBlockHeight))
    }

    override fun saveBalance(balance: String, address: String) {
        database.balanceDao().insert(EthereumBalance(address, balance))
    }

    override fun saveTransactions(ethereumTransactions: List<EthereumTransaction>) {
        database.transactionDao().insert(ethereumTransactions)
    }

    override fun clear() {
        database.clearAllTables()
    }
}
