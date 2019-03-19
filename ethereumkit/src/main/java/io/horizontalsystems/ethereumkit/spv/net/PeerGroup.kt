package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.les.LESPeer
import java.math.BigInteger

class PeerGroup(network: INetwork,
                storage: ISpvStorage,
                connectionKey: ECKey,
                address: String) : LESPeer.Listener {
    interface Listener {
        fun onUpdate(state: AccountState)
    }

    private var syncPeer: LESPeer
    private val connectionKey: ECKey
    private val storage: ISpvStorage
    private var address: ByteArray

    private val headersLimit = 50
    private var syncing = false

    lateinit var listener: Listener

    init {
        this.storage = storage
        this.connectionKey = connectionKey
        this.address = address.hexStringToByteArray()

        val node = Node(id = "f9a9a1b2f68dc119b0f44ba579cbc40da1f817ddbdb1045a57fa8159c51eb0f826786ce9e8b327d04c9ad075f2c52da90e9f84ee4dde3a2a911bb1270ef23f6d".hexStringToByteArray(),
                host = "eth-testnet.horizontalsystems.xyz",
                port = 20303,
                discoveryPort = 30301)

        /*val node = Node(id = "e679038c2e4f9f764acd788c3935cf526f7f630b55254a122452e63e2cfae3066ca6b6c44082c2dfbe9ddffc9df80546d40ef38a0e3dfc9c8720c732446ca8f3".hexStringToByteArray(),
                host = "192.168.4.39",
                port = 30303,
                discoveryPort = 30301)*/

        /* val node = Node(id = "053d2f57829e5785d10697fa6c5333e4d98cc564dbadd87805fd4fedeb09cbcb642306e3a73bd4191b27f821fb442fcf964317d6a520b29651e7dd09d1beb0ec".hexStringToByteArray(),
                 host = "79.98.29.154",
                 port = 30303,
                 discoveryPort = 30301)*/

        val lastBlockHeader = storage.getLastBlockHeader() ?: run {
            storage.saveBlockHeaders(listOf(network.checkpointBlock))
            network.checkpointBlock
        }

        syncPeer = LESPeer.getInstance(network, lastBlockHeader, connectionKey, node)
        syncPeer.listener = this
    }


//------------------Public methods----------------------

    fun start() {
        syncPeer.connect()
    }

    fun stop() {
        syncPeer.disconnect(null)
    }

    private fun syncBlocks() {
        storage.getLastBlockHeader()?.let {
            syncPeer.requestBlockHeaders(it.height, headersLimit)
        }
    }

//-----------------LESPeer.Listener methods----------------

    override fun didConnect() {
        println("PeerGroup -> didConnect\n")
        syncing = true
        syncBlocks()
    }

    override fun didReceive(blockHeaders: List<BlockHeader>, blockHeight: BigInteger) {
        println("PeerGroup -> didReceive\n")

        storage.saveBlockHeaders(blockHeaders)

        if (blockHeaders.size < headersLimit) {
            println("blocks synced!\n")

            syncing = false

            storage.getLastBlockHeader()?.let { lastBlock ->
                syncPeer.requestAccountState(address, lastBlock)
            }

            return
        }

        syncBlocks()
    }

    override fun didReceive(accountState: AccountState, address: ByteArray, blockHeader: BlockHeader) {
        listener.onUpdate(accountState)
        println(accountState)
    }

    override fun didAnnounce(blockHash: ByteArray, blockHeight: BigInteger) {
        if (!syncing) {
            syncing = true
            syncBlocks()
        }
    }
}
