package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.les.LESPeer

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

    lateinit var listener: Listener

    init {
        this.storage = storage
        this.connectionKey = connectionKey
        this.address = address.hexStringToByteArray()

        /*val node = Node(id = "1baf02c18c08ab0d009ccc9b51168be6a8776509ff229a6ca08507b53579cb99e0df1709bd1bcf64aed348f9a31298842cf12c1764c8de9d28abb921a548ad8c".hexStringToByteArray(),
                      host = "eth-testnet.horizontalsystems.xyz",
                      port = 20303,
                      discoveryPort = 30301)*/

        val node = Node(id = "e679038c2e4f9f764acd788c3935cf526f7f630b55254a122452e63e2cfae3066ca6b6c44082c2dfbe9ddffc9df80546d40ef38a0e3dfc9c8720c732446ca8f3".hexStringToByteArray(),
                host = "192.168.4.39",
                port = 30303,
                discoveryPort = 30301)

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
            syncPeer.requestBlockHeaders(it.hashHex)
        }
    }

//-----------------LESPeer.Listener methods----------------

    override fun didConnect() {
        println("PeerGroup -> didConnect\n")
        syncBlocks()
    }

    override fun didReceive(blockHeaders: List<BlockHeader>, blockHash: ByteArray) {
        println("PeerGroup -> didReceive\n")

        if (blockHeaders.size <= 1) {
            println("blocks synced!\n")

            storage.getLastBlockHeader()?.let { lastBlock ->
                syncPeer.requestAccountState(address, lastBlock)
            }

            return
        }

        storage.saveBlockHeaders(blockHeaders)
        syncBlocks()
    }

    override fun didReceive(accountState: AccountState, address: ByteArray, blockHeader: BlockHeader) {
        listener.onUpdate(accountState)
        println(accountState)
    }
}
