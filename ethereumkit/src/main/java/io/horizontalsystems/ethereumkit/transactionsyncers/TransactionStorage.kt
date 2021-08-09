package io.horizontalsystems.ethereumkit.transactionsyncers

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerStateStorage
import io.horizontalsystems.ethereumkit.core.storage.TransactionDatabase
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single

class TransactionStorage(database: TransactionDatabase) : ITransactionStorage, ITransactionSyncerStateStorage {
    private val notSyncedTransactionDao = database.notSyncedTransactionDao()
    private val transactionDao = database.transactionDao()
    private val tagsDao = database.transactionTagDao()
    private val notSyncedInternalTransactionDao = database.notSyncedInternalTransactionDao()
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

    //region NotSyncedTransaction
    override fun getNotSyncedInternalTransactions(): NotSyncedInternalTransaction? {
        return notSyncedInternalTransactionDao.getAll().firstOrNull()
    }

    override fun add(notSyncedInternalTransaction: NotSyncedInternalTransaction) {
        notSyncedInternalTransactionDao.insert(notSyncedInternalTransaction)
    }

    override fun remove(notSyncedInternalTransaction: NotSyncedInternalTransaction) {
        notSyncedInternalTransactionDao.delete(notSyncedInternalTransaction)
    }
//endregion

    //region Transaction
    override fun getTransactionHashes(): List<ByteArray> {
        return transactionDao.getTransactionHashes()
    }

    override fun getTransactionsBeforeAsync(
        tags: List<List<String>>,
        hash: ByteArray?,
        limit: Int?
    ): Single<List<FullTransaction>> {
        val whereConditions = mutableListOf<String>()

        if (tags.isNotEmpty()) {
            val tagConditions = tags
                .mapIndexed { index, andTags ->
                    val tagsString = andTags.joinToString(", ") { "'$it'" }
                    "transaction_tags_$index.name IN ($tagsString)"
                }
                .joinToString(" AND ")

            whereConditions.add(tagConditions)
        }

        hash?.let { transactionDao.getTransaction(hash) }?.let { fromTransaction ->
            val transactionIndex = fromTransaction.receiptWithLogs?.receipt?.transactionIndex ?: 0
            val fromCondition = """
                           (
                                tx.timestamp < ${fromTransaction.transaction.timestamp} OR 
                                (
                                    tx.timestamp = ${fromTransaction.transaction.timestamp} AND 
                                    receipt.transactionIndex < $transactionIndex
                                )
                           )
                           """

            whereConditions.add(fromCondition)
        }

        val transactionTagJoinStatements = tags
            .mapIndexed { index, _ ->
                "INNER JOIN TransactionTag AS transaction_tags_$index ON tx.hash = transaction_tags_$index.hash"
            }
            .joinToString("\n")

        val limitClause = limit?.let { "LIMIT $limit" } ?: ""

        val orderClause = """
                          ORDER BY tx.timestamp DESC,
                          receipt.transactionIndex DESC
                          """

        val whereClause =
            if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""

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
        val whereConditions = mutableListOf<String>()
        var transactionTagJoinStatements = ""

        if (tags.isNotEmpty()) {
            val tagCondition = tags
                .mapIndexed { index, andTags ->
                    val tagsString = andTags.joinToString(", ") { "'$it'" }
                    "transaction_tags_$index.name IN ($tagsString)"
                }
                .joinToString(" AND ")

            whereConditions.add(tagCondition)

            transactionTagJoinStatements += tags
                .mapIndexed { index, _ ->
                    "INNER JOIN TransactionTag AS transaction_tags_$index ON tx.hash = transaction_tags_$index.hash"
                }
                .joinToString("\n")
        }

        whereConditions.add(
            """
            receipt.status IS NULL
            """
        )

        val whereClause =
            if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""

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

    override fun save(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    override fun getPendingTransactions(fromTransaction: Transaction?): List<Transaction> {
        return transactionDao.getPendingTransactions().filter {
            fromTransaction == null || it.nonce > fromTransaction.nonce || (it.nonce == fromTransaction.nonce && it.timestamp > fromTransaction.timestamp)
        }
    }

    override fun getPendingTransactionList(nonce: Long): List<Transaction> {
        return transactionDao.getPendingTransactionList(nonce)
    }

    override fun addDroppedTransaction(droppedTransaction: DroppedTransaction) {
        transactionDao.insert(droppedTransaction)
    }

    //endregion

    //region InternalTransactions

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

    override fun remove(logs: List<TransactionLog>) {
        transactionDao.deleteLogs(logs)
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
