package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.storage.TransactionDatabase
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single

interface IStorage {
    fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction>
    fun addNotSyncedTransactions(transactions: List<NotSyncedTransaction>)
    fun update(notSyncedTransaction: NotSyncedTransaction)
    fun remove(transaction: NotSyncedTransaction)

    fun getTransactions(hashes: List<ByteArray>): List<FullTransaction>
    fun getTransactionHashes(): List<ByteArray>
    fun getEtherTransactionsAsync(address: Address, fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>>
    fun save(transaction: Transaction)

    fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt?
    fun save(transactionReceipt: TransactionReceipt)

    fun save(logs: List<TransactionLog>)

}

class Storage(
        private val database: TransactionDatabase
) : IStorage { // TODO rename to TransactionStorage

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
    override fun getTransactionHashes(): List<ByteArray> {
        return transactionDao.getTransactionHashes()
    }

    override fun getEtherTransactionsAsync(address: Address, fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>> {
        return transactionDao.getTransactionsAsync()
                .map { fullTransactions ->
                    var etherTransactions = fullTransactions.filter { it.hasEtherTransfer(address) }

                    fromHash?.let {
                        val fullTxFrom = etherTransactions.firstOrNull { it.transaction.hash.contentEquals(fromHash) }
                        fullTxFrom?.let {
                            etherTransactions = etherTransactions.filter {
                                it.transaction.timestamp < fullTxFrom.transaction.timestamp ||
                                        (it.transaction.timestamp == fullTxFrom.transaction.timestamp
                                                && (it.receiptWithLogs?.receipt?.transactionIndex?.compareTo(fullTxFrom.receiptWithLogs?.receipt?.transactionIndex ?: 0) ?: 0) < 0)
                            }
                        }
                    }
                    limit?.let {
                        etherTransactions = etherTransactions.take(limit)
                    }
                    etherTransactions
                }
    }

    override fun getTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return transactionDao.getTransactions(hashes)
    }

    override fun save(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    //endregion

    //region TransactionReceipt
    override fun save(transactionReceipt: TransactionReceipt) {
       transactionDao.insert(transactionReceipt)
    }

    override fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt? {
        return transactionDao.getTransactionReceipt(transactionHash)
    }
    //endregion

    //region TransactionLog
    override fun save(logs: List<TransactionLog>) {
        transactionDao.insert(logs)
    }
    //endregion
}
