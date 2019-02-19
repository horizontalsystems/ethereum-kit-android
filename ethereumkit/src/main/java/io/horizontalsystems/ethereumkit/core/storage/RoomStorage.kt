package io.horizontalsystems.ethereumkit.core.storage

import android.content.Context
import io.horizontalsystems.ethereumkit.core.IStorage
import io.horizontalsystems.ethereumkit.models.BalanceRoom
import io.horizontalsystems.ethereumkit.models.GasPriceRoom
import io.horizontalsystems.ethereumkit.models.LastBlockHeightRoom
import io.horizontalsystems.ethereumkit.models.TransactionRoom
import io.reactivex.Single
import java.math.BigDecimal

class RoomStorage(databaseName: String, context: Context) : IStorage {

    private val database: KitDatabase = KitDatabase.getInstance(context, databaseName)


    override fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<TransactionRoom>> {
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

    override fun getGasPrice(): BigDecimal? {
        return database.gasPriceDao().getGasPrice()?.gasPriceInGwei
    }

    override fun saveLastBlockHeight(lastBlockHeight: Int) {
        database.lastBlockHeight().insert(LastBlockHeightRoom(lastBlockHeight))
    }

    override fun saveGasPrice(gasPrice: BigDecimal) {
        database.gasPriceDao().insert(GasPriceRoom(gasPrice))
    }

    override fun saveBalance(balance: BigDecimal, address: String) {
        database.balanceDao().insert(BalanceRoom(address, balance))
    }

    override fun saveTransactions(transactions: List<TransactionRoom>) {
        database.transactionDao().insert(transactions)
    }

    override fun clear() {
        database.clearAllTables()
    }
}
