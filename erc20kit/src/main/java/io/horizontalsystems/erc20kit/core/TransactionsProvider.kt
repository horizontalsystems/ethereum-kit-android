package io.horizontalsystems.erc20kit.core

//class TransactionsProvider(private val dataProvider: IDataProvider) : ITransactionsProvider {
//
//    override fun getTransactions(contractAddress: Address, address: Address, startBlock: Long, endBlock: Long): Single<List<Transaction>> {
//        return dataProvider.getTransactionLogs(contractAddress, address, startBlock, endBlock)
//                .map { logs ->
//                    logs.mapNotNull { getTransactionFromLog(it) }
//                }
//    }
//
//    private fun getTransactionFromLog(log: TransactionLog): Transaction? {
//        val value = log.data.hexStringToByteArray().toBigInteger()
//        val from = Address(log.topics[1].hexStringToByteArray().copyOfRange(12, 32))
//        val to = Address(log.topics[2].hexStringToByteArray().copyOfRange(12, 32))
//
//        val transaction = Transaction(
//                transactionHash = log.transactionHash.hexStringToByteArray(),
//                interTransactionIndex = log.logIndex,
//                transactionIndex = log.transactionIndex,
//                from = from,
//                to = to,
//                value = value,
//                timestamp = log.timestamp ?: System.currentTimeMillis() / 1000)
//
//        transaction.logIndex = log.logIndex
//        transaction.blockHash = log.blockHash.hexStringToByteArray()
//        transaction.blockNumber = log.blockNumber
//
//        return transaction
//    }
//
//}
