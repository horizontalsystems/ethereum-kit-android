package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.Eip20Event
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderEip1155Transaction
import io.horizontalsystems.ethereumkit.models.ProviderEip721Transaction
import io.horizontalsystems.ethereumkit.models.ProviderInternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTokenTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTransaction
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionLog
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.ethereumkit.spv.models.AccountStateSpv
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.reactivex.Single
import java.math.BigInteger


interface IApiStorage {
    fun getLastBlockHeight(): Long?
    fun saveLastBlockHeight(lastBlockHeight: Long)

    fun getAccountState(): AccountState?
    fun saveAccountState(state: AccountState)
}

interface ISpvStorage {
    fun getLastBlockHeader(): BlockHeader?
    fun saveBlockHeaders(blockHeaders: List<BlockHeader>)
    fun getBlockHeadersReversed(fromBlockHeight: Long, limit: Int): List<BlockHeader>

    fun getAccountState(): AccountStateSpv?
    fun saveAccountSate(accountState: AccountStateSpv)
}

interface IBlockchain {
    val source: String
    var listener: IBlockchainListener?

    fun start()
    fun refresh()
    fun stop()
    fun syncAccountState()

    val syncState: EthereumKit.SyncState
    val lastBlockHeight: Long?
    val accountState: AccountState?

    fun send(rawTransaction: RawTransaction, signature: Signature): Single<Transaction>
    fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long>
    fun estimateGas(to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: GasPrice?, data: ByteArray?): Single<Long>
    fun getTransactionReceipt(transactionHash: ByteArray): Single<RpcTransactionReceipt>
    fun getTransaction(transactionHash: ByteArray): Single<RpcTransaction>
    fun getBlock(blockNumber: Long): Single<RpcBlock>

    fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<TransactionLog>>
    fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray>
    fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray>

    fun <T> rpcSingle(rpc: JsonRpc<T>): Single<T>
}

interface IBlockchainListener {
    fun onUpdateLastBlockHeight(lastBlockHeight: Long)
    fun onUpdateSyncState(syncState: EthereumKit.SyncState)
    fun onUpdateAccountState(accountState: AccountState)
}

interface ITransactionStorage {
    fun getTransactions(hashes: List<ByteArray>): List<Transaction>
    fun getTransaction(hash: ByteArray): Transaction?
    fun getTransactionsBeforeAsync(tags: List<List<String>>, hash: ByteArray?, limit: Int?): Single<List<Transaction>>
    fun save(transactions: List<Transaction>)

    fun getPendingTransactions(): List<Transaction>
    fun getPendingTransactions(tags: List<List<String>>): List<Transaction>
    fun getNonPendingTransactionsByNonces(from: Address, pendingTransactionNonces: List<Long>): List<Transaction>

    fun getLastInternalTransaction(): InternalTransaction?
    fun getInternalTransactions(): List<InternalTransaction>
    fun getInternalTransactionsByHashes(hashes: List<ByteArray>): List<InternalTransaction>
    fun saveInternalTransactions(internalTransactions: List<InternalTransaction>)

    fun saveTags(tags: List<TransactionTag>)
    fun getDistinctTokenContractAddresses(): List<String>

    fun getTransactionsAfterSingle(hash: ByteArray?): Single<List<Transaction>>
}

interface IEip20Storage {
    fun getLastEvent(): Eip20Event?
    fun save(events: List<Eip20Event>)
    fun getEvents(): List<Eip20Event>
    fun getEventsByHashes(hashes: List<ByteArray>): List<Eip20Event>
}

interface ITransactionSyncer {
    fun getTransactionsSingle(): Single<Pair<List<Transaction>, Boolean>>
}

interface IMethodDecorator {
    fun contractMethod(input: ByteArray): ContractMethod?
}

interface IEventDecorator {
    fun contractEventInstancesMap(transactions: List<Transaction>): Map<String, List<ContractEventInstance>>
    fun contractEventInstances(logs: List<TransactionLog>): List<ContractEventInstance>
}

interface IExtraDecorator {
    fun extra(hash: ByteArray) : Map<String, Any>
}

interface ITransactionDecorator {
    fun decoration(
        from: Address?,
        to: Address?,
        value: BigInteger?,
        contractMethod: ContractMethod?,
        internalTransactions: List<InternalTransaction>,
        eventInstances: List<ContractEventInstance>
    ): TransactionDecoration?
}

interface ITransactionProvider {
    fun getTransactions(startBlock: Long): Single<List<ProviderTransaction>>
    fun getInternalTransactions(startBlock: Long): Single<List<ProviderInternalTransaction>>
    fun getInternalTransactionsAsync(hash: ByteArray): Single<List<ProviderInternalTransaction>>
    fun getTokenTransactions(startBlock: Long): Single<List<ProviderTokenTransaction>>
    fun getEip721Transactions(startBlock: Long): Single<List<ProviderEip721Transaction>>
    fun getEip1155Transactions(startBlock: Long): Single<List<ProviderEip1155Transaction>>
}

interface INonceProvider {
    fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long>
}
