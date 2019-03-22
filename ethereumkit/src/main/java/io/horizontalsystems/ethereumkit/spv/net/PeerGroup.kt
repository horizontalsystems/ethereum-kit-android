package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

class PeerGroup(val storage: ISpvStorage,
                val peerProvider: PeerProvider,
                val blockValidator: BlockValidator,
                val blockHelper: BlockHelper,
                val state: PeerGroupState,
                val address: ByteArray,
                val headersLimit: Int = 75) : IPeerListener {

    interface Listener {
        fun onUpdate(accountState: AccountState)
        fun onUpdate(syncState: SyncState)
    }

    val syncState: SyncState
        get() = state.syncState

    var listener: Listener? = null

//------------------Public methods----------------------

    fun start() {
        val peer = peerProvider.getPeer()
        peer.listener = this

        state.syncPeer = peer
        state.syncState = SyncState.Syncing

        listener?.onUpdate(SyncState.Syncing)

        peer.connect()
    }

    fun stop() {
//        syncPeer.disconnect(null)
    }

//-----------------LESPeer.Listener methods----------------

    override fun didConnect() {
        println("PeerGroup -> didConnect\n")

        state.syncPeer?.requestBlockHeaders(blockHelper.lastBlockHeader, headersLimit)
    }

    override fun didDisconnect(error: Throwable?) {
        state.syncPeer = null
    }

    @Throws(PeerException::class)
    private fun handleFork(blockHeaders: List<BlockHeader>, fromBlockHeader: BlockHeader) {
        val localHeaders = storage.getBlockHeadersReversed(fromBlockHeader.height, blockHeaders.size)

        val forkedHeader = localHeaders.firstOrNull { localHeader ->
            blockHeaders.any { it.hashHex.contentEquals(localHeader.hashHex) && it.height == localHeader.height }
        } ?: throw InvalidPeer()

        state.syncPeer?.requestBlockHeaders(forkedHeader, headersLimit)
    }

    @Throws(BlockValidator.BlockValidationError::class)
    private fun handleBlockHeaders(blockHeaders: List<BlockHeader>, blockHeader: BlockHeader) {

        blockValidator.validate(blockHeaders, blockHeader)

        storage.saveBlockHeaders(blockHeaders)

        if (blockHeaders.size == headersLimit) {
            state.syncPeer?.requestBlockHeaders(blockHeaders.last(), headersLimit)
        } else {
            state.syncPeer?.requestAccountState(address, blockHelper.lastBlockHeader)
        }
    }

    override fun didReceive(blockHeaders: List<BlockHeader>, blockHeader: BlockHeader, reversed: Boolean) {
        try {
            if (reversed) {
                handleFork(blockHeaders, blockHeader)
            } else {
                handleBlockHeaders(blockHeaders, blockHeader)
            }
        } catch (error: Exception) {
            when (error) {
                is InvalidPeer -> state.syncPeer?.disconnect(error)
                is BlockValidator.InvalidChain -> state.syncPeer?.disconnect(error)
                is BlockValidator.InvalidProofOfWork -> state.syncPeer?.disconnect(error)
                is BlockValidator.ForkDetected -> state.syncPeer?.requestBlockHeaders(blockHeader, headersLimit, true)
            }
        }
    }

    override fun didReceive(accountState: AccountState, address: ByteArray, blockHeader: BlockHeader) {
        listener?.onUpdate(accountState)

        state.syncState = SyncState.Synced

        listener?.onUpdate(SyncState.Synced)

        println(accountState)
    }

    override fun didAnnounce(blockHash: ByteArray, blockHeight: Long) {
        if (state.syncState == SyncState.Synced) {
            state.syncPeer?.requestBlockHeaders(blockHelper.lastBlockHeader, headersLimit)
        }
    }

    open class PeerException : Exception()
    class InvalidPeer : PeerException()

}
