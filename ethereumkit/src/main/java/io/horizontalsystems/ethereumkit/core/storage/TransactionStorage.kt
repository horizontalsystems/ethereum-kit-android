package io.horizontalsystems.ethereumkit.core.storage

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.reactivex.Single

class TransactionStorage(database: TransactionDatabase) : ITransactionStorage {
    private val transactionDao = database.transactionDao()
    private val tagsDao = database.transactionTagDao()

    override fun getTransactions(hashes: List<ByteArray>): List<Transaction> =
        transactionDao.getTransactions(hashes)

    override fun getTransaction(hash: ByteArray): Transaction? =
        transactionDao.getTransaction(hash)

    override fun getTransactionsBeforeAsync(tags: List<List<String>>, hash: ByteArray?, limit: Int?): Single<List<Transaction>> {
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
            val transactionIndex = fromTransaction.transactionIndex ?: 0
            val fromCondition = """
                           (
                                tx.timestamp < ${fromTransaction.timestamp} OR 
                                (
                                    tx.timestamp = ${fromTransaction.timestamp} AND 
                                    tx.transactionIndex < $transactionIndex
                                ) OR
                                (
                                    tx.timestamp = ${fromTransaction.timestamp} AND
                                    tx.transactionIndex = $transactionIndex AND
                                    HEX(tx.hash) < "${fromTransaction.hash.toRawHexString().uppercase()}"
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

        val whereClause = if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""
        val orderClause = "ORDER BY tx.timestamp DESC, tx.transactionIndex DESC, HEX(tx.hash) DESC"
        val limitClause = limit?.let { "LIMIT $limit" } ?: ""

        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` as tx
                      $transactionTagJoinStatements
                      $whereClause
                      $orderClause
                      $limitClause
                      """

        return transactionDao.getTransactionsByRawQuery(SimpleSQLiteQuery(sqlQuery))
    }

    override fun save(transactions: List<Transaction>) {
        transactionDao.insert(transactions)
    }

    override fun getPendingTransactions(): List<Transaction> =
        transactionDao.getPendingTransactions()

    override fun getPendingTransactions(tags: List<List<String>>): List<Transaction> {
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
            "tx.blockNumber IS NULL"
        )

        val whereClause =
            if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""

        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` as tx
                      $transactionTagJoinStatements
                      $whereClause
                      """

        return transactionDao.getPending(SimpleSQLiteQuery(sqlQuery))
    }

    override fun getNonPendingTransactionsByNonces(from: Address, pendingTransactionNonces: List<Long>): List<Transaction> =
        transactionDao.getNonPendingByNonces(from.raw, pendingTransactionNonces)

    override fun getLastInternalTransaction(): InternalTransaction? =
        transactionDao.getLastInternalTransaction()

    override fun getInternalTransactions(): List<InternalTransaction> =
        transactionDao.getInternalTransactions()

    override fun getInternalTransactionsByHashes(hashes: List<ByteArray>): List<InternalTransaction> =
        transactionDao.getInternalTransactionsByHashes(hashes)

    override fun saveInternalTransactions(internalTransactions: List<InternalTransaction>) {
        transactionDao.insertInternalTransactions(internalTransactions)
    }

    override fun saveTags(tags: List<TransactionTag>) {
        tagsDao.insert(tags)
    }

    override fun getDistinctTokenContractAddresses(): List<String> {
        return tagsDao.getDistinctTokenContractAddresses()
    }

    override fun getTransactionsAfterSingle(hash: ByteArray?): Single<List<Transaction>> {
        val whereConditions = mutableListOf<String>()
        hash?.let { transactionDao.getTransaction(hash) }?.let { fromTransaction ->
            val transactionIndex = fromTransaction.transactionIndex ?: 0
            val fromCondition = """
                           (
                                tx.timestamp > ${fromTransaction.timestamp} OR 
                                (
                                    tx.timestamp = ${fromTransaction.timestamp} AND 
                                    tx.transactionIndex > $transactionIndex
                                ) OR
                                (
                                    tx.timestamp = ${fromTransaction.timestamp} AND
                                    tx.transactionIndex = $transactionIndex AND
                                    HEX(tx.hash) > "${fromTransaction.hash.toRawHexString().uppercase()}"
                                )
                           )
                           """

            whereConditions.add(fromCondition)
        }

        val whereClause = if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""
        val orderClause = "ORDER BY tx.timestamp, tx.transactionIndex, HEX(tx.hash)"

        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` as tx
                      $whereClause
                      $orderClause
                      """

        return transactionDao.getTransactionsByRawQuery(SimpleSQLiteQuery(sqlQuery))
    }
}
