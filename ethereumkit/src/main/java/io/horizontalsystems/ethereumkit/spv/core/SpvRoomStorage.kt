package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.core.room.SPVDatabase
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.reactivex.Single

class SpvRoomStorage(private val database: SPVDatabase) : ISpvStorage {

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

    override fun getTransactions(fromHash: ByteArray?, limit: Int?, contractAddress: ByteArray?): Single<List<EthereumTransaction>> {
        return database.transactionDao().getTransactions()
    }

    override fun saveTransactions(transactions: List<EthereumTransaction>) {
        database.transactionDao().insert(transactions)
    }
}
