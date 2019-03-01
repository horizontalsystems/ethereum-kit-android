package io.horizontalsystems.ethereumkit.light.core.storage

import android.content.Context
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.light.models.BlockHeader
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

class SpvRoomStorage : ISpvStorage {

    val database: SpvDatabase

    constructor(database: SpvDatabase) {
        this.database = database
    }

    constructor(context: Context, databaseName: String) {
        this.database = SpvDatabase.getInstance(context, databaseName)
    }

    override fun getLastBlockHeight(): Int? {
        return getLastBlockHeader()?.height?.toInt()
    }

    override fun getBalance(address: String): String? {
        return null
    }

    override fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>> {


        return database.transactionDao().getTransactions()
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

    override fun clear() {
        database.clearAllTables()
    }

    override fun getLastBlockHeader(): BlockHeader? {
        return database.blockHeaderDao().getAll().firstOrNull()
    }

    override fun saveBlockHeaders(blockHeaders: List<BlockHeader>) {
        return database.blockHeaderDao().insertAll(blockHeaders)
    }
}
