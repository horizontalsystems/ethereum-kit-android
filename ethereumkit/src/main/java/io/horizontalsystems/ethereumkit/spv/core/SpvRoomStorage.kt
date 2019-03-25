package io.horizontalsystems.ethereumkit.spv.core

import android.content.Context
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.spv.core.room.SPVDatabase
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

class SpvRoomStorage : ISpvStorage {

    val database: SPVDatabase

    constructor(database: SPVDatabase) {
        this.database = database
    }

    constructor(context: Context, databaseName: String) {
        this.database = SPVDatabase.getInstance(context, databaseName)
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

    override fun getAccountState(): AccountState? {
        return database.accountStateDao().getAccountState()
    }

    override fun saveAccountSate(accountState: AccountState) {
        database.accountStateDao().insert(accountState)
    }
}
