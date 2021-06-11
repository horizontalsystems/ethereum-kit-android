package io.horizontalsystems.ethereumkit.transactionsyncers

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerStateStorage
import io.horizontalsystems.ethereumkit.core.storage.TransactionDatabase
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single
import java.util.concurrent.atomic.AtomicLong

class TransactionStorage(database: TransactionDatabase) : ITransactionStorage, ITransactionSyncerStateStorage {
    private val notSyncedTransactionDao = database.notSyncedTransactionDao()
    private val transactionDao = database.transactionDao()
    private val tagsDao = database.transactionTagDao()
    private val transactionSyncerStateDao = database.transactionSyncerStateDao()

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
                                                && (it.receiptWithLogs?.receipt?.transactionIndex?.compareTo(fullTxFrom.receiptWithLogs?.receipt?.transactionIndex
                                                ?: 0) ?: 0) < 0)
                            }
                        }
                    }
                    limit?.let {
                        etherTransactions = etherTransactions.take(limit)
                    }
                    etherTransactions
                }
    }

    override fun getTransactionsBeforeAsync(tags: List<List<String>>, hash: ByteArray?, limit: Int?): Single<List<FullTransaction>> {

        var whereClause = "WHERE " + tags
                .mapIndexed { index, andTags ->
                    val tagsString = andTags.joinToString(", ") { "'$it'" }
                    "transaction_tags_$index.name IN ($tagsString)"
                }
                .joinToString(" AND ")

        hash?.let { transactionDao.getTransaction(hash) }?.let { fromTransaction ->
            val transactionIndex = fromTransaction.receiptWithLogs?.receipt?.transactionIndex ?: 0
            whereClause += """
                           AND tx.timestamp < ${fromTransaction.transaction.timestamp} OR 
                                (
                                    tx.timestamp = ${fromTransaction.transaction.timestamp} AND 
                                    receipt.transactionIndex < $transactionIndex
                                )
                           )
                           """
        }

        val limitClause = limit?.let { "LIMIT $limit" } ?: ""

        val orderClause = """
                          ORDER BY tx.timestamp DESC,
                          receipt.transactionIndex DESC
                          """

        val transactionTagJoinStatements = tags
                .mapIndexed { index, _ ->
                    "INNER JOIN TransactionTag AS transaction_tags_$index ON tx.hash = transaction_tags_$index.hash"
                }
                .joinToString("\n")

        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` as tx
                      $transactionTagJoinStatements
                      LEFT JOIN TransactionReceipt as receipt ON tx.hash = receipt.transactionHash
                      $whereClause
                      GROUP BY tx.hash
                      $orderClause
                      $limitClause
                      """

        return transactionDao.getTransactionsBeforeAsync(SimpleSQLiteQuery(sqlQuery))
    }

    override fun getPendingTransactions(tags: List<List<String>>): List<FullTransaction> {

        var whereClause = "WHERE " + tags
                .mapIndexed { index, andTags ->
                    val tagsString = andTags.joinToString(", ") { "'$it'" }
                    "transaction_tags_$index.name IN ($tagsString)"
                }
                .joinToString(" AND ")


        whereClause += """
                           AND receipt.status == NULL
                           """

        val transactionTagJoinStatements = tags
                .mapIndexed { index, _ ->
                    "INNER JOIN TransactionTag AS transaction_tags_$index ON tx.hash = transaction_tags_$index.hash"
                }
                .joinToString("\n")


        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` as tx
                      $transactionTagJoinStatements
                      LEFT JOIN TransactionReceipt as receipt ON tx.hash = receipt.transactionHash
                      $whereClause
                      GROUP BY tx.hash
                      """

        return transactionDao.getPending(SimpleSQLiteQuery(sqlQuery))
    }

    override fun set(tags: List<TransactionTag>) {
        tags.forEach {
            tagsDao.insert(it)
        }
    }

    override fun getFullTransaction(hash: ByteArray): FullTransaction? {
        return transactionDao.getTransaction(hash)
    }

    override fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return transactionDao.getTransactions(hashes)
    }

    override fun getFullTransactions(fromSyncOrder: Long?): List<FullTransaction> {
        return transactionDao.getTransactions(fromSyncOrder ?: 0)
    }

    private val lastSyncOrder = AtomicLong(transactionDao.getLastTransactionSyncOrder() ?: 0)

    override fun save(transaction: Transaction) {
        transaction.syncOrder = lastSyncOrder.incrementAndGet()
        transactionDao.insert(transaction)
    }

    override fun getPendingTransactions(fromTransaction: Transaction?): List<Transaction> {
        return transactionDao.getPendingTransactions().filter {
            fromTransaction == null || it.nonce > fromTransaction.nonce || (it.nonce == fromTransaction.nonce && it.timestamp > fromTransaction.timestamp)
        }
    }

    override fun getPendingTransaction(nonce: Long): Transaction? {
        return transactionDao.getPendingTransaction(nonce)
    }

    override fun addDroppedTransaction(droppedTransaction: DroppedTransaction) {
        transactionDao.insert(droppedTransaction)
    }

    //endregion

    //region InternalTransactions
    override fun getLastInternalTransactionBlockHeight(): Long? {
        return transactionDao.getLastInternalTransactionBlockNumber()
    }

    override fun saveInternalTransactions(internalTransactions: List<InternalTransaction>) {
        transactionDao.insertInternalTransactions(internalTransactions)
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

    //region TransactionSyncerState
    override fun getTransactionSyncerState(id: String): TransactionSyncerState? {
        return transactionSyncerStateDao.getTransactionSyncerState(id)
    }

    override fun save(transactionSyncerState: TransactionSyncerState) {
        transactionSyncerStateDao.insert(transactionSyncerState)
    }
    //endregion

}
