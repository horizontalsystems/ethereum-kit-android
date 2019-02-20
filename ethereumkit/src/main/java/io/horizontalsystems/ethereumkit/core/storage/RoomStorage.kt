package io.horizontalsystems.ethereumkit.core.storage

import android.content.Context
import io.horizontalsystems.ethereumkit.core.IStorage
import io.horizontalsystems.ethereumkit.models.EthereumBalance
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import java.math.BigDecimal

class RoomStorage(databaseName: String, context: Context) : IStorage {

    private val database: KitDatabase = KitDatabase.getInstance(context, databaseName)


    override fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>> {
        return database.transactionDao().getTransactions(contractAddress)
                .flatMap { transactions ->

                    var filtered = if (contractAddress.isNullOrEmpty()) {
                        transactions.filter { it.input == "0x" }
                    } else {
                        transactions
                    }

                    fromHash?.let { fromHash ->
                        val tx = transactions.firstOrNull { it.hash == fromHash }
                        tx?.timeStamp?.let { txTimeStamp ->
                            filtered = filtered.filter { it.timeStamp < txTimeStamp }
                        }
                    }

                    limit?.let {
                        filtered = filtered.take(it)
                    }

                    Single.just(filtered)
                }
    }

    override fun getBalance(address: String): BigDecimal {
        return database.balanceDao().getBalance(address)?.balance ?: BigDecimal.ZERO
    }

    override fun getLastBlockHeight(): Int? {
        return database.lastBlockHeight().getLastBlockHeight()?.height
    }

    override fun getLastTransactionBlockHeight(isErc20: Boolean): Int? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getGasPriceInWei(): Long? {
        return database.gasPriceDao().getGasPrice()?.gasPriceInGwei
    }

    override fun saveLastBlockHeight(lastBlockHeight: Int) {
        database.lastBlockHeight().insert(LastBlockHeight(lastBlockHeight))
    }

    override fun saveGasPriceInWei(gasPriceInWei: Long) {
        database.gasPriceDao().insert(GasPrice(gasPriceInWei))
    }

    override fun saveBalance(balance: BigDecimal, address: String) {
        database.balanceDao().insert(EthereumBalance(address, balance))
    }

    override fun saveTransactions(ethereumTransactions: List<EthereumTransaction>) {
        database.transactionDao().insert(ethereumTransactions)
    }

    override fun clear() {
        database.clearAllTables()
    }
}
