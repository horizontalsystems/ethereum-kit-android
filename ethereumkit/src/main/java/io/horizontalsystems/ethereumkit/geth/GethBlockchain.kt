package io.horizontalsystems.ethereumkit.geth

//import io.horizontalsystems.ethereumkit.core.*
//import io.horizontalsystems.ethereumkit.models.EthereumLog
//import io.horizontalsystems.ethereumkit.models.EthereumTransaction
//import io.horizontalsystems.ethereumkit.network.INetwork
//import io.horizontalsystems.ethereumkit.network.Ropsten
//import io.horizontalsystems.ethereumkit.models.RawTransaction
//import io.reactivex.Single
//import io.reactivex.disposables.CompositeDisposable
//import io.reactivex.schedulers.Schedulers
//import io.reactivex.subjects.PublishSubject
//import org.ethereum.geth.*
//import org.slf4j.LoggerFactory
//import java.math.BigInteger
//import java.util.concurrent.TimeUnit
//
//class GethBlockchain private constructor(private val node: Node,
//                                         private val network: INetwork,
//                                         private val storage: IApiStorage,
//                                         private val transactionSigner: TransactionSigner,
//                                         private val transactionBuilder: TransactionBuilder,
//                                         private val address: Address) : IBlockchain, NewHeadHandler {
//
//    private val logger = LoggerFactory.getLogger(GethBlockchain::class.java)
//
//    private val context: Context = Geth.newContext()
//    private val lastBlockHeightSubject = PublishSubject.create<Long>()
//    private val disposables = CompositeDisposable()
//
//    init {
//        lastBlockHeightSubject.sample(1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
//                .subscribe {
//                    onUpdateLastBlockHeight(it)
//                }.let {
//                    disposables.add(it)
//                }
//    }
//
//    private fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
//        storage.saveLastBlockHeight(lastBlockHeight)
//        listener?.onUpdateLastBlockHeight(lastBlockHeight)
//
//        try {
//            val syncProgress = node.ethereumClient.syncProgress(context)
//
//            val startingBlock = syncProgress.startingBlock.toDouble()
//            val highestBlock = syncProgress.highestBlock.toDouble()
//            val currentBlock = syncProgress.currentBlock.toDouble()
//
//            val progress = (currentBlock - startingBlock) / (highestBlock - startingBlock)
//
//            syncState = EthereumKit.SyncState.Syncing(progress)
//
//        } catch (error: Exception) {
//            val peersCount = node.peersInfo?.size() ?: 0
//
//            if (peersCount == 0L) {
//                syncState = EthereumKit.SyncState.Syncing()
//            } else {
//                syncState = EthereumKit.SyncState.Synced()
//
//                syncAccountState()
//            }
//        }
//    }
//
//    private fun syncAccountState() {
//        try {
//            logger.debug("syncAccountState")
//
//            val gethBalance = node.ethereumClient.getBalanceAt(context, address, -1).string()
//            val balance = BigInteger(gethBalance)
//
//            logger.debug("balance: $balance")
//            onUpdateBalance(balance)
//
//        } catch (error: Exception) {
//            logger.error("Could not fetch account state", error)
//        }
//    }
//
//    private fun onUpdateBalance(balance: BigInteger) {
//        storage.saveBalance(balance)
//        listener?.onUpdateBalance(balance)
//    }
//
//    private fun sendInternal(rawTransaction: RawTransaction): EthereumTransaction {
//        val nonce = node.ethereumClient.getNonceAt(context, address, -1)
//
//
//        val toAccount = Address(rawTransaction.to)
//
//        val amount = BigInt(0)
//        amount.setString(rawTransaction.value.toString(10), 10)
//
//        val gethTransaction = Transaction(nonce, toAccount, amount, rawTransaction.gasLimit,
//                BigInt(rawTransaction.gasPrice), rawTransaction.data)
//
//        val signatureData = transactionSigner.sign(rawTransaction, nonce)
//        val signedTransaction = gethTransaction.withSignature(signatureData, BigInt(network.id.toLong()))
//
//        logger.debug("signatureData: ${signatureData.toHexString()}")
//
//        logger.debug("nonce: ${signedTransaction.nonce}")
//        logger.debug("toAccount: ${signedTransaction.to.hex}")
//        logger.debug("value: ${signedTransaction.value}")
//        logger.debug("gasLimit: ${signedTransaction.gas}")
//        logger.debug("gasPrice: ${signedTransaction.gasPrice}")
//        logger.debug("data: ${signedTransaction.data}")
//
//        node.ethereumClient.sendTransaction(context, signedTransaction)
//
//        return transactionBuilder.transaction(rawTransaction, nonce, transactionSigner.signature(signatureData))
//    }
//
//    private fun convertToLog(gethLog: Log?): EthereumLog? {
//        val address = gethLog?.address?.bytes ?: return null
//
//        val blockHash = gethLog.blockHash.bytes ?: return null
//
//        val data = gethLog.data ?: return null
//
//        val gethTopics = gethLog.topics ?: return null
//
//        val transactionHash = gethLog.txHash?.bytes ?: return null
//
//        val topics: MutableList<ByteArray> = mutableListOf()
//
//        for (i in 0 until gethTopics.size()) {
//            val gethTopic = try {
//                gethTopics.get(i)
//            } catch (error: Exception) {
//                logger.error("gethTopics.get($i) error", error)
//                return null
//            }
//
//            topics.add(gethTopic.bytes)
//        }
//
//        return EthereumLog(address.toHexString(), blockHash.toHexString(),
//                gethLog.blockNumber,
//                data.toHexString(),
//                gethLog.index.toInt(),
//                false,
//                topics.map { it.toHexString() },
//                transactionHash.toHexString(),
//                gethLog.txIndex.toInt())
//    }
//
//    private fun getLogsInternal(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): List<EthereumLog> {
//        logger.debug("Get logs: ${address?.toHexString()} $fromBlock -- $toBlock, topics: ${topics.size}")
//
//        val addresses = Addresses()
//        addresses.append(Address(address))
//
//        val gethTopics = Topics(topics.size.toLong())
//
//        topics.forEachIndexed { index, topic ->
//            topic?.let {
//                val hashes = Hashes()
//                hashes.append(Hash(topic))
//                gethTopics.set(index.toLong(), hashes)
//            }
//        }
//
//        val query = FilterQuery()
//        query.addresses = addresses
//        query.fromBlock = BigInt(fromBlock)
//        query.toBlock = BigInt(toBlock)
//        query.topics = gethTopics
//
//        val ethLogs = node.ethereumClient.filterLogs(context, query)
//
//        logger.debug("Eth logs result: ${ethLogs.size()}")
//
//        val logs: MutableList<EthereumLog> = mutableListOf()
//
//        for (i in 0 until ethLogs.size()) {
//            val log = try {
//                ethLogs.get(i)
//            } catch (error: Exception) {
//                logger.error("ethLogs.get($i) error", error)
//                null
//            }
//
//            convertToLog(log)?.let {
//                logs.add(it)
//            }
//        }
//
//        logger.debug("Logs result: ${logs.size}")
//
//        return logs
//    }
//
//    private fun callInternal(contractAddress: ByteArray, data: ByteArray, blockHeight: Long?): ByteArray {
//        logger.debug("Calling ${contractAddress.toHexString()}, blockHeight: $blockHeight")
//
//        val message = CallMsg()
//        message.to = Address(contractAddress)
//        message.data = data
//
//        val resultData = node.ethereumClient.callContract(context, message, blockHeight ?: -1)
//
//        logger.debug("Call result: ${resultData.toHexString()}")
//
//        return resultData
//    }
//
//    //-------------------IBlockchain-----------------------
//
//    override var listener: IBlockchainListener? = null
//
//    override fun start() {
//        try {
//            node.start()
//            logger.debug("GethBlockchain: started")
//        } catch (error: Exception) {
//            logger.error("GethBlockchain: failed to start node", error)
//        }
//
//        try {
//            node.ethereumClient.subscribeNewHead(context, this, 16)
//            logger.debug("GethBlockchain: subscribed to new headers")
//        } catch (error: Exception) {
//            logger.error("GethBlockchain: failed to subscribe to new headers", error)
//        }
//    }
//
//    override fun refresh() {
//
//    }
//
//    override fun stop() {
//        try {
//            node.stop()
//            logger.debug("GethBlockchain: stopped")
//        } catch (error: Exception) {
//            logger.error("GethBlockchain: failed to stop node", error)
//        }
//    }
//
//    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.Syncing()
//        private set (value) {
//            if (value != field) {
//                field = value
//                listener?.onUpdateSyncState(field)
//            }
//        }
//
//    override val lastBlockHeight: Long?
//        get() = storage.getLastBlockHeight()
//
//
//    override val balance: BigInteger?
//        get() = storage.getBalance()
//
//    override fun send(rawTransaction: RawTransaction): Single<EthereumTransaction> {
//        return Single.create { emitter ->
//            try {
//                val transaction = sendInternal(rawTransaction)
//                emitter.onSuccess(transaction)
//
//            } catch (error: Exception) {
//                logger.error("Send error", error)
//                emitter.onError(error)
//            }
//        }
//    }
//
//    override fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>> {
//        return Single.create { emitter ->
//            try {
//                val logs = getLogsInternal(address, topics, fromBlock, toBlock, pullTimestamps)
//                emitter.onSuccess(logs)
//
//            } catch (error: Exception) {
//                logger.error("GetLogs error", error)
//                emitter.onError(error)
//            }
//        }
//    }
//
//    override fun getStorageAt(contractAddress: ByteArray, position: ByteArray, blockNumber: Long): Single<ByteArray> {
//        TODO("not implemented")
//    }
//
//    override fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<ByteArray> {
//        return Single.create { emitter ->
//            try {
//                val callResult = callInternal(contractAddress, data, blockNumber)
//                emitter.onSuccess(callResult)
//
//            } catch (error: Exception) {
//                logger.error("Call error", error)
//                emitter.onError(error)
//            }
//        }
//    }
//
//    //-------------Geth.NewHeadHandler------------
//
//    override fun onNewHead(header: Header?) {
//        val blockNumber = header?.number ?: return
//
//        lastBlockHeightSubject.onNext(blockNumber)
//    }
//
//    override fun onError(error: String?) {
//        logger.error("NewHeadHandler error", error)
//    }
//
//    companion object {
//
//        fun getInstance(nodeDirectory: String,
//                        network: INetwork,
//                        storage: IApiStorage,
//                        transactionSigner: TransactionSigner,
//                        transactionBuilder: TransactionBuilder,
//                        address: ByteArray): GethBlockchain {
//
//            val nodeConfig = NodeConfig().apply {
//                ethereumGenesis = if (network is Ropsten) Geth.testnetGenesis() else Geth.mainnetGenesis()
//                ethereumNetworkID = network.id.toLong()
//                bootstrapNodes = Geth.foundationBootnodes()
//                ethereumEnabled = true
//                maxPeers = 25
//                whisperEnabled = false
//            }
//
//            val node = Geth.newNode(nodeDirectory, nodeConfig)
//
//            return GethBlockchain(node, network, storage, transactionSigner, transactionBuilder, Address(address))
//        }
//    }
//
//}
