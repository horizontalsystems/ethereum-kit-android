package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.storage.TransactionDatabase
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single

interface IStorage {
    fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction>
    fun addNotSyncedTransactions(transactions: List<NotSyncedTransaction>)
    fun update(notSyncedTransaction: NotSyncedTransaction)
    fun remove(transaction: NotSyncedTransaction)

    fun getHashesFromTransactions(): List<ByteArray>
    fun getEtherTransactions(address: Address, fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>>
    fun save(transaction: Transaction)

    fun getTransactionReceipt(): TransactionReceipt?
    fun save(transactionReceipt: TransactionReceipt)

    fun save(logs: List<TransactionLog>)

}

class Storage(
        private val database: TransactionDatabase
) : IStorage { //TODO rename to TransactionStorage

    private val notSyncedTransactionDao = database.notSyncedTransactionDao()
    private val transactionDao = database.transactionDao()

    //region NotSyncedTransaction
    override fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction> {
        return notSyncedTransactionDao.get(limit).map { it.asNotSyncedTransaction() }
    }

    override fun addNotSyncedTransactions(transactions: List<NotSyncedTransaction>) {
        notSyncedTransactionDao.insert(transactions.map { NotSyncTransactionRecord(it) })
    }

    override fun update(notSyncedTransaction: NotSyncedTransaction) {
        notSyncedTransactionDao.insert(NotSyncTransactionRecord(notSyncedTransaction))
    }

    override fun remove(transaction: NotSyncedTransaction) {
        notSyncedTransactionDao.deleteByHash(transaction.hash)
    }
    //endregion

    //region Transaction
    override fun getHashesFromTransactions(): List<ByteArray> {
        return transactionDao.getTransactionHashes()
    }

    override fun getEtherTransactions(address: Address, fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>> {
        TODO("not implemented")
    }

    override fun save(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    //endregion

    //region TransactionReceipt
    override fun save(transactionReceipt: TransactionReceipt) {
        TODO("not implemented")
    }

    override fun getTransactionReceipt(): TransactionReceipt? {
        TODO("not implemented")
    }
    //endregion

    //region TransactionLog
    override fun save(logs: List<TransactionLog>) {
        TODO("not implemented")
    }
    //endregion
}
