package io.horizontalsystems.ethereumkit.spv.core

import android.content.Context
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.core.room.SPVDatabase
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.reactivex.Single

class SpvRoomStorage : ISpvStorage {

    val database: SPVDatabase

    constructor(database: SPVDatabase) {
        this.database = database
    }

    constructor(context: Context, databaseName: String) {
        this.database = SPVDatabase.getInstance(context, databaseName)
    }

    override fun getLastBlockHeight(): Long? {
        return getLastBlockHeader()?.height
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

    override fun getBlockHeadersReversed(fromBlockHeight: Long, limit: Int): List<BlockHeader> {
        return database.blockHeaderDao().getByBlockHeightRange(fromBlockHeight - limit, fromBlockHeight)
    }
}
