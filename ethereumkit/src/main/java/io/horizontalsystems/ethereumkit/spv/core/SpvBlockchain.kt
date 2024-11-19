package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.api.core.IRpcApiProvider
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.core.IBlockchainListener
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.network.INetwork
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import io.horizontalsystems.ethereumkit.spv.models.AccountStateSpv
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.BlockHelper
import io.horizontalsystems.ethereumkit.spv.net.BlockValidator
import io.horizontalsystems.ethereumkit.spv.net.PeerGroup
import io.horizontalsystems.ethereumkit.spv.net.PeerProvider
import io.horizontalsystems.ethereumkit.spv.net.handlers.*
import io.horizontalsystems.ethereumkit.spv.net.tasks.HandshakeTask
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.logging.Logger

class SpvBlockchain(
        private val peer: IPeer,
        private val blockSyncer: BlockSyncer,
        private val accountStateSyncer: AccountStateSyncer,
        private val transactionSender: TransactionSender,
        private val storage: ISpvStorage,
        private val network: INetwork,
        private val rpcApiProvider: IRpcApiProvider
) : IBlockchain, IPeerListener,
    BlockSyncer.Listener, AccountStateSyncer.Listener, TransactionSender.Listener {

    private val logger = Logger.getLogger("SpvBlockchain")

    private val sendingTransactions: MutableMap<Int, PublishSubject<Transaction>> = HashMap()

    //--------------IBlockchain---------------------

    override val source: String
        get() = "SPV"

    override var listener: IBlockchainListener? = null

    override fun start() {
        logger.info("SpvBlockchain started")

        peer.connect()
    }

    override fun stop() {
        TODO("not implemented")
    }

    override fun refresh() {
        TODO("not implemented")
    }

    override fun syncAccountState() {
        TODO("not implemented")
    }

    override var syncState = SyncState.NotSynced(SyncError.NotStarted())

    override val lastBlockHeight: Long?
        get() = storage.getLastBlockHeader()?.height

    override val accountState: AccountState?
        get() = storage.getAccountState()?.let { AccountState(it.balance, it.nonce) }

    override fun send(rawTransaction: RawTransaction, signature: Signature): Single<Transaction> {
        return try {
            val sendId = RandomHelper.randomInt()
            transactionSender.send(sendId, peer, rawTransaction, signature)
            val subject = PublishSubject.create<Transaction>()
            sendingTransactions[sendId] = subject
            Single.fromFuture(subject.toFuture())

        } catch (error: Throwable) {
            Single.error(error)
        }
    }

    override fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long> {
        TODO("not implemented")
    }

    override fun estimateGas(to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: GasPrice?, data: ByteArray?): Single<Long> {
        TODO("not implemented")
    }

    override fun getTransactionReceipt(transactionHash: ByteArray): Single<RpcTransactionReceipt> {
        TODO("not implemented")
    }

    override fun getTransaction(transactionHash: ByteArray): Single<RpcTransaction> {
        TODO("not implemented")
    }

    override fun getBlock(blockNumber: Long): Single<RpcBlock> {
        TODO("not implemented")
    }

    override fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<TransactionLog>> {
        TODO("not implemented")
    }

    override fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        TODO("not implemented")
    }

    override fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        TODO("not implemented")
    }

    override fun <T> rpcSingle(rpc: JsonRpc<T>): Single<T> {
        TODO("not implemented")
    }

    //-------------IPeerListener--------------------

    override fun didConnect(peer: IPeer) {
        val lastBlockHeader = storage.getLastBlockHeader() ?: network.checkpointBlock
        peer.add(HandshakeTask(peer.id, network, lastBlockHeader))
    }

    override fun didDisconnect(peer: IPeer, error: Throwable?) {
        TODO("not implemented")
    }

    //------------BlockSyncer.Listener--------------

    override fun onSuccess(taskPerformer: ITaskPerformer, lastBlockHeader: BlockHeader) {
        logger.info("Blocks synced successfully up to ${lastBlockHeader.height}. Starting account state sync...")

        accountStateSyncer.sync(taskPerformer, lastBlockHeader)
    }

    override fun onFailure(error: Throwable) {
        logger.info("Blocks sync failed: ${error.message}")
    }

    override fun onUpdate(lastBlockHeader: BlockHeader) {
        listener?.onUpdateLastBlockHeight(lastBlockHeader.height)
    }

    //-------------AccountStateSyncer.Listener------------------

    override fun onUpdate(accountState: AccountStateSpv) {
        listener?.onUpdateAccountState(accountState.let { AccountState(it.balance, it.nonce) })
    }

    //---------------TransactionSender.Listener------------------

    override fun onSendSuccess(sendId: Int, transaction: Transaction) {
        val subject = sendingTransactions.remove(sendId) ?: return

        subject.onNext(transaction)
        subject.onComplete()
    }

    override fun onSendFailure(sendId: Int, error: Throwable) {
        val subject = sendingTransactions.remove(sendId) ?: return

        subject.onError(error)
    }

    companion object {
        fun getInstance(storage: ISpvStorage, transactionBuilder: TransactionBuilder, rpcApiProvider: IRpcApiProvider, network: INetwork, address: Address, nodeKey: ECKey): SpvBlockchain {
            val peerProvider = PeerProvider(nodeKey, storage, network)
            val blockValidator = BlockValidator()
            val blockHelper = BlockHelper(storage, network)
            val peer = PeerGroup(peerProvider)

            val blockSyncer = BlockSyncer(storage, blockHelper, blockValidator)
            val accountStateSyncer = AccountStateSyncer(storage, address)
            val transactionSender = TransactionSender(transactionBuilder)

            val spvBlockchain = SpvBlockchain(peer, blockSyncer, accountStateSyncer, transactionSender, storage, network, rpcApiProvider)

            peer.listener = spvBlockchain
            blockSyncer.listener = spvBlockchain
            accountStateSyncer.listener = spvBlockchain
            transactionSender.listener = spvBlockchain

            val handshakeHandler = HandshakeTaskHandler(blockSyncer)
            peer.register(taskHandler = handshakeHandler)
            peer.register(messageHandler = handshakeHandler)

            val blockHeadersHandler = BlockHeadersTaskHandler(blockSyncer)
            peer.register(taskHandler = blockHeadersHandler)
            peer.register(messageHandler = blockHeadersHandler)

            val accountStateHandler = AccountStateTaskHandler(accountStateSyncer)
            peer.register(taskHandler = accountStateHandler)
            peer.register(messageHandler = accountStateHandler)

            val sendTransactionHandler = SendTransactionTaskHandler(transactionSender)
            peer.register(taskHandler = sendTransactionHandler)
            peer.register(messageHandler = sendTransactionHandler)

            val announcedBlockHandler = AnnouncedBlockHandler(blockSyncer)
            peer.register(announcedBlockHandler)

            return spvBlockchain
        }
    }

    open class SendError : Exception()
    class NoAccountState : SendError()
}
