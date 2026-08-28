package io.horizontalsystems.ethereumkit.core

import android.app.Application
import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.core.ApiRpcSyncer
import io.horizontalsystems.ethereumkit.api.core.IRpcSyncer
import io.horizontalsystems.ethereumkit.api.core.NodeWebSocket
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchain
import io.horizontalsystems.ethereumkit.api.core.WebSocketRpcSyncer
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.storage.Eip20Storage
import io.horizontalsystems.ethereumkit.core.storage.TransactionStorage
import io.horizontalsystems.ethereumkit.core.storage.TransactionSyncerStateStorage
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.decorations.EthereumDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionLog
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.ethereumkit.network.AddressTypeAdapter
import io.horizontalsystems.ethereumkit.network.BigIntegerTypeAdapter
import io.horizontalsystems.ethereumkit.network.BlockscoutService
import io.horizontalsystems.ethereumkit.network.ByteArrayTypeAdapter
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import io.horizontalsystems.ethereumkit.network.DefaultBlockParameterTypeAdapter
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.ethereumkit.network.IntTypeAdapter
import io.horizontalsystems.ethereumkit.network.LongTypeAdapter
import io.horizontalsystems.ethereumkit.network.OptionalTypeAdapter
import io.horizontalsystems.ethereumkit.transactionsyncers.EthereumTransactionSyncer
import io.horizontalsystems.ethereumkit.transactionsyncers.InternalTransactionSyncer
import io.horizontalsystems.ethereumkit.transactionsyncers.TransactionSyncManager
import io.horizontalsystems.hdwalletkit.Mnemonic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.Security
import java.util.Objects
import java.util.Optional
import java.util.logging.Logger

class EthereumKit(
    private val blockchain: IBlockchain,
    private val nonceProvider: NonceProvider,
    val transactionManager: TransactionManager,
    private val transactionSyncManager: TransactionSyncManager,
    private val connectionManager: ConnectionManager,
    private val address: Address,
    val chain: Chain,
    val walletId: String,
    val transactionProvider: ITransactionProvider,
    val eip20Storage: IEip20Storage,
    private val decorationManager: DecorationManager,
    private val state: EthereumKitState = EthereumKitState()
) : IBlockchainListener {

    private val logger = Logger.getLogger("EthereumKit")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val lastBlockHeightSubject = bufferedSharedFlow<Long>()
    private val syncStateSubject = bufferedSharedFlow<SyncState>()
    private val accountStateSubject = bufferedSharedFlow<AccountState>()

    val defaultGasLimit: Long = 21_000
    private val defaultMinAmount: BigInteger = BigInteger.ONE

    private var started = false

    init {
        state.lastBlockHeight = blockchain.lastBlockHeight
        state.accountState = blockchain.accountState

        transactionManager.fullTransactionsFlow
            .onEach {
                blockchain.syncAccountState()
            }
            .launchIn(scope)
    }

    val lastBlockHeight: Long?
        get() = state.lastBlockHeight

    val accountState: AccountState?
        get() = state.accountState

    val syncState: SyncState
        get() = blockchain.syncState

    val transactionsSyncState: SyncState
        get() = transactionSyncManager.syncState

    val receiveAddress: Address
        get() = address

    val lastBlockHeightFlow: Flow<Long>
        get() = lastBlockHeightSubject

    val syncStateFlow: Flow<SyncState>
        get() = syncStateSubject

    val transactionsSyncStateFlow: Flow<SyncState>
        get() = transactionSyncManager.syncStateFlow

    val accountStateFlow: Flow<AccountState>
        get() = accountStateSubject

    val allTransactionsFlow: Flow<Pair<List<FullTransaction>, Boolean>>
        get() = transactionManager.fullTransactionsFlow

    fun start() {
        if (started)
            return
        started = true

        blockchain.start()
        transactionSyncManager.sync()
    }

    fun stop() {
        started = false
        blockchain.stop()
        state.clear()
        connectionManager.stop()
    }

    fun onEnterForeground() {
        blockchain.resume()
    }

    fun onEnterBackground() {
        blockchain.pause()
    }

    fun refresh() {
        blockchain.refresh()
        transactionSyncManager.sync()
    }

    suspend fun getNonce(defaultBlockParameter: DefaultBlockParameter): Long {
        return nonceProvider.getNonce(defaultBlockParameter)
    }

    fun getFullTransactionsFlow(tags: List<List<String>>): Flow<List<FullTransaction>> {
        return transactionManager.getFullTransactionsFlow(tags)
    }

    suspend fun getFullTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): List<FullTransaction> {
        return transactionManager.getFullTransactionsAsync(tags, fromHash, limit)
    }

    fun getPendingFullTransactions(tags: List<List<String>>): List<FullTransaction> {
        return transactionManager.getPendingFullTransactions(tags)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return transactionManager.getFullTransactions(hashes)
    }

    suspend fun getFullTransaction(hash: ByteArray): FullTransaction {
        return transactionManager.getFullTransaction(hash)
    }

    suspend fun getFullTransactionsAfter(hash: ByteArray?): List<FullTransaction> {
        return transactionManager.getFullTransactionsAfter(hash)
    }

    suspend fun estimateGas(to: Address?, value: BigInteger, gasPrice: GasPrice): Long {
        // without address - provide default gas limit
        if (to == null) {
            return defaultGasLimit
        }

        // if amount is 0 - set default minimum amount
        val resolvedAmount = if (value == BigInteger.ZERO) defaultMinAmount else value

        return blockchain.estimateGas(to, resolvedAmount, chain.gasLimit, gasPrice, null)
    }

    suspend fun estimateGas(to: Address?, value: BigInteger?, gasPrice: GasPrice?, data: ByteArray?): Long {
        return blockchain.estimateGas(to, value, chain.gasLimit, gasPrice, data)
    }

    suspend fun estimateGas(transactionData: TransactionData, gasPrice: GasPrice? = null): Long {
        return estimateGas(transactionData.to, transactionData.value, gasPrice, transactionData.input)
    }

    suspend fun rawTransaction(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long? = null
    ): RawTransaction {
        return rawTransaction(
            address = transactionData.to,
            value = transactionData.value,
            transactionInput = transactionData.input,
            gasPrice = gasPrice,
            gasLimit = gasLimit,
            nonce = nonce
        )
    }

    suspend fun rawTransaction(
        address: Address,
        value: BigInteger,
        transactionInput: ByteArray = byteArrayOf(),
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long? = null
    ): RawTransaction {
        val resolvedNonce = nonce ?: nonceProvider.getNonce(DefaultBlockParameter.Pending)

        return RawTransaction(gasPrice, gasLimit, address, value, resolvedNonce, transactionInput)
    }

    suspend fun send(rawTransaction: RawTransaction, signature: Signature): FullTransaction {
        logger.info("send rawTransaction: $rawTransaction")

        val transaction = blockchain.send(rawTransaction, signature)
        return withContext(Dispatchers.IO) {
            transactionManager.handle(listOf(transaction)).first()
        }
    }

    fun decorate(transactionData: TransactionData): TransactionDecoration? {
        return decorationManager.decorateTransaction(address, transactionData)
    }

    fun transferTransactionData(address: Address, value: BigInteger): TransactionData {
        return transactionManager.etherTransferTransactionData(address = address, value = value)
    }

    suspend fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): List<TransactionLog> {
        return blockchain.getLogs(address, topics, fromBlock, toBlock, pullTimestamps)
    }

    suspend fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): ByteArray {
        return blockchain.getStorageAt(contractAddress, position, defaultBlockParameter)
    }

    suspend fun call(
        contractAddress: Address,
        data: ByteArray,
        defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest
    ): ByteArray {
        return blockchain.call(contractAddress, data, defaultBlockParameter)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: $address")
        return lines.joinToString { "\n" }
    }

    fun statusInfo(): Map<String, Any> {
        val statusInfo = LinkedHashMap<String, Any>()

        statusInfo["Last Block Height"] = state.lastBlockHeight ?: "N/A"
        statusInfo["Sync State"] = blockchain.syncState.toString()
        statusInfo["Blockchain source"] = blockchain.source
        statusInfo["Transactions source"] = "Infura, Etherscan" //TODO

        return statusInfo
    }

    fun getTagTokenContractAddresses(): List<String> {
        return transactionManager.getDistinctTokenContractAddresses()
    }

    //
    //IBlockchainListener
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        if (state.lastBlockHeight == lastBlockHeight)
            return

        state.lastBlockHeight = lastBlockHeight
        lastBlockHeightSubject.tryEmit(lastBlockHeight)
        transactionSyncManager.sync()
    }

    override fun onUpdateSyncState(syncState: SyncState) {
        syncStateSubject.tryEmit(syncState)
    }

    override fun onUpdateAccountState(accountState: AccountState) {
        if (state.accountState == accountState) return

        state.accountState = accountState
        accountStateSubject.tryEmit(accountState)
    }

    fun addTransactionSyncer(transactionSyncer: ITransactionSyncer) {
        transactionSyncManager.add(transactionSyncer)
    }

    fun addNonceProvider(provider: INonceProvider) {
        nonceProvider.addProvider(provider)
    }

    fun addExtraDecorator(decorator: IExtraDecorator) {
        decorationManager.addExtraDecorator(decorator)
    }

    fun addMethodDecorator(decorator: IMethodDecorator) {
        decorationManager.addMethodDecorator(decorator)
    }

    fun addEventDecorator(decorator: IEventDecorator) {
        decorationManager.addEventDecorator(decorator)
    }

    fun addTransactionDecorator(decorator: ITransactionDecorator) {
        decorationManager.addTransactionDecorator(decorator)
    }

    internal suspend fun <T: Any> rpc(rpc: JsonRpc<T>): T {
        return blockchain.rpc(rpc)
    }

    sealed class SyncState {
        class Synced : SyncState()
        class NotSynced(val error: Throwable) : SyncState()
        class Syncing(val progress: Double? = null) : SyncState()

        override fun toString(): String = when (this) {
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""}"
            is NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
            else -> this.javaClass.simpleName
        }

        override fun equals(other: Any?): Boolean {
            if (other !is SyncState)
                return false

            if (other.javaClass != this.javaClass)
                return false

            if (other is Syncing && this is Syncing) {
                return other.progress == this.progress
            }

            return true
        }

        override fun hashCode(): Int {
            if (this is Syncing) {
                return Objects.hashCode(this.progress)
            }
            return Objects.hashCode(this.javaClass.name)
        }
    }

    open class SyncError : Exception() {
        class NotStarted : SyncError()
        class NoNetworkConnection : SyncError()
    }

    companion object {

        val gson = GsonBuilder()
            .setLenient()
            .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
            .registerTypeAdapter(Long::class.java, LongTypeAdapter())
            .registerTypeAdapter(object : TypeToken<Long?>() {}.type, LongTypeAdapter())
            .registerTypeAdapter(Int::class.java, IntTypeAdapter())
            .registerTypeAdapter(ByteArray::class.java, ByteArrayTypeAdapter())
            .registerTypeAdapter(Address::class.java, AddressTypeAdapter())
            .registerTypeHierarchyAdapter(DefaultBlockParameter::class.java, DefaultBlockParameterTypeAdapter())
            .registerTypeAdapter(
                object : TypeToken<Optional<RpcTransaction>>() {}.type,
                OptionalTypeAdapter<RpcTransaction>(RpcTransaction::class.java)
            )
            .registerTypeAdapter(
                object : TypeToken<Optional<RpcTransactionReceipt>>() {}.type,
                OptionalTypeAdapter<RpcTransactionReceipt>(RpcTransactionReceipt::class.java)
            )
            .registerTypeAdapter(object : TypeToken<Optional<RpcBlock>>() {}.type, OptionalTypeAdapter<RpcBlock>(RpcBlock::class.java))
            .create()

        suspend fun call(
            rpcSource: RpcSource,
            contractAddress: Address,
            data: ByteArray,
            defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest
        ): ByteArray {
            val rpcApiProvider = RpcApiProviderFactory.nodeApiProvider(rpcSource)
            val rpc = RpcBlockchain.callRpc(contractAddress, data, defaultBlockParameter)
            return rpcApiProvider.execute(rpc)
        }

        suspend fun estimateGas(
            rpcSource: RpcSource,
            chain: Chain,
            from: Address,
            to: Address?,
            value: BigInteger?,
            gasPrice: GasPrice,
            data: ByteArray?
        ): Long {
            return RpcBlockchain.estimateGas(rpcSource, from, to, value, chain.gasLimit, gasPrice, data)
        }

        suspend fun estimateGas(
            rpcSource: RpcSource,
            chain: Chain,
            from: Address,
            transactionData: TransactionData,
            gasPrice: GasPrice
        ): Long {
            return estimateGas(rpcSource, chain, from, transactionData.to, transactionData.value, gasPrice, transactionData.input)
        }

        fun init() {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(InternalBouncyCastleProvider.getInstance())
        }

        fun getInstance(
            application: Application,
            words: List<String>,
            passphrase: String = "",
            chain: Chain,
            rpcSource: RpcSource,
            transactionSource: TransactionSource,
            walletId: String
        ): EthereumKit {
            val seed = Mnemonic().toSeed(words, passphrase)
            val privateKey = Signer.privateKey(seed, chain)
            val address = ethereumAddress(privateKey)
            return getInstance(application, address, chain, rpcSource, transactionSource, walletId)
        }

        fun getInstance(
            application: Application,
            address: Address,
            chain: Chain,
            rpcSource: RpcSource,
            transactionSource: TransactionSource,
            walletId: String
        ): EthereumKit {

            val connectionManager = ConnectionManager(application)

            val syncer: IRpcSyncer = when (rpcSource) {
                is RpcSource.WebSocket -> {
                    val rpcWebSocket = NodeWebSocket(rpcSource.uri, gson, rpcSource.auth)
                    val webSocketRpcSyncer = WebSocketRpcSyncer(rpcWebSocket, gson)

                    rpcWebSocket.listener = webSocketRpcSyncer

                    webSocketRpcSyncer
                }

                is RpcSource.Http -> {
                    val apiProvider = RpcApiProviderFactory.nodeApiProvider(rpcSource)
                    ApiRpcSyncer(apiProvider, connectionManager, chain.syncInterval)
                }
            }

            val transactionBuilder = TransactionBuilder(address, chain.id)
            val transactionProvider = transactionProvider(transactionSource, address, chain.id)

            val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(application, walletId, chain)
            val storage = ApiStorage(apiDatabase)

            val blockchain = RpcBlockchain.instance(address, storage, syncer, transactionBuilder)

            val transactionDatabase = EthereumDatabaseManager.getTransactionDatabase(application, walletId, chain)
            val transactionStorage = TransactionStorage(transactionDatabase)
            val transactionSyncerStateStorage = TransactionSyncerStateStorage(transactionDatabase)

            val erc20Database = EthereumDatabaseManager.getErc20Database(application, walletId, chain)
            val erc20Storage = Eip20Storage(erc20Database)

            val ethereumTransactionSyncer = EthereumTransactionSyncer(transactionProvider, transactionSyncerStateStorage)
            val internalTransactionsSyncer = InternalTransactionSyncer(transactionProvider, transactionStorage)

            val decorationManager = DecorationManager(address, transactionStorage)
            val transactionManager = TransactionManager(address, transactionStorage, decorationManager, blockchain, transactionProvider)
            val transactionSyncManager = TransactionSyncManager(transactionManager)

            transactionSyncManager.add(internalTransactionsSyncer)
            transactionSyncManager.add(ethereumTransactionSyncer)

            val nonceProvider = NonceProvider()
            nonceProvider.addProvider(blockchain)

            val ethereumKit = EthereumKit(
                blockchain,
                nonceProvider,
                transactionManager,
                transactionSyncManager,
                connectionManager,
                address,
                chain,
                walletId,
                transactionProvider,
                erc20Storage,
                decorationManager
            )

            blockchain.listener = ethereumKit

            decorationManager.addTransactionDecorator(EthereumDecorator(address))

            return ethereumKit
        }

        fun clear(context: Context, chain: Chain, walletId: String) {
            EthereumDatabaseManager.clear(context, chain, walletId)
        }

        private fun transactionProvider(transactionSource: TransactionSource, address: Address, chainId: Int): ITransactionProvider {
            return when (val type = transactionSource.type) {
                is TransactionSource.SourceType.Etherscan -> {
                    val service = EtherscanService(type.apiBaseUrl, type.apiKeys, chainId)
                    EtherscanTransactionProvider(service, address)
                }
                is TransactionSource.SourceType.Blockscout -> {
                    val service = BlockscoutService(type.apiBaseUrl, type.apiKeys)
                    BlockscoutTransactionProvider(service, address)
                }
            }
        }

        private fun ethereumAddress(privateKey: BigInteger): Address {
            val publicKey = CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            return Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray())
        }

    }

}
