package io.horizontalsystems.ethereumkit.api.storage

import io.horizontalsystems.ethereumkit.api.models.EthereumBalance
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.core.IApiStorage
import java.math.BigInteger

class ApiStorage(private val database: ApiDatabase) : IApiStorage {

    override fun getBalance(): BigInteger? {
        return database.balanceDao().getBalance()?.balance
    }

    override fun getLastBlockHeight(): Long? {
        return database.lastBlockHeightDao().getLastBlockHeight()?.height
    }

    override fun saveLastBlockHeight(lastBlockHeight: Long) {
        database.lastBlockHeightDao().insert(LastBlockHeight(lastBlockHeight))
    }

    override fun saveBalance(balance: BigInteger) {
        database.balanceDao().insert(EthereumBalance(balance))
    }

}
