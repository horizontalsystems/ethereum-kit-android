package io.horizontalsystems.ethereumkit.api.storage

import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.core.IApiStorage

class ApiStorage(
        private val database: ApiDatabase
) : IApiStorage {

    override fun getLastBlockHeight(): Long? {
        return database.lastBlockHeightDao().getLastBlockHeight()?.height
    }

    override fun saveLastBlockHeight(lastBlockHeight: Long) {
        database.lastBlockHeightDao().insert(LastBlockHeight(lastBlockHeight))
    }

    override fun saveAccountState(state: AccountState) {
        database.balanceDao().insert(state)
    }

    override fun getAccountState(): AccountState? {
        return database.balanceDao().getAccountState()
    }

}
