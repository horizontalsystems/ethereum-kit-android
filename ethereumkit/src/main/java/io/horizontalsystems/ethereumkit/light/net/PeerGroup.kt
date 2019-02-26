package io.horizontalsystems.ethereumkit.light.net

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.light.models.BlockHeader
import io.horizontalsystems.ethereumkit.light.net.les.IPeerListener
import io.horizontalsystems.ethereumkit.light.net.les.Peer
import io.horizontalsystems.ethereumkit.light.net.les.messages.ProofsMessage
import java.math.BigInteger

class PeerGroup(network: INetwork, address: String) : IPeerListener {


    private var syncPeer: Peer
    private var blockHeaders = mutableListOf<BlockHeader>()
    private var address: ByteArray

    init {
        val myKey = CryptoUtils.ecKeyFromPrivate(BigInteger("38208918395832628331087730025239389699013035341486183519748173810236817397977"))

        /*val node = Node(id = "1baf02c18c08ab0d009ccc9b51168be6a8776509ff229a6ca08507b53579cb99e0df1709bd1bcf64aed348f9a31298842cf12c1764c8de9d28abb921a548ad8c".hexStringToByteArray(),
                host = "eth-testnet.horizontalsystems.xyz",
                port = 20303,
                discoveryPort = 30301)*/

        val node = Node(id = "e679038c2e4f9f764acd788c3935cf526f7f630b55254a122452e63e2cfae3066ca6b6c44082c2dfbe9ddffc9df80546d40ef38a0e3dfc9c8720c732446ca8f3".hexStringToByteArray(),
                host = "192.168.4.39",
                port = 30303,
                discoveryPort = 30301)

//        this.address = address.substring(2).hexStringToByteArray()
        this.address = "f757461bdc25ee2b047d545a50768e52d530b750".hexStringToByteArray()
//        this.address = "f757461bdc25ee2b047d545a50768e52d530b751".hexStringToByteArray()
//        this.address = "37531e574427BDE92d9B3a3c2291D9A004827435".hexStringToByteArray()
//        this.address = "1b763c4b9632d6876D83B2270fF4d01b792DE479".hexStringToByteArray()
//        this.address = "401CB37eFa5d82dC51FB599e6A4B1D2b3aaeb2B2".hexStringToByteArray()

        syncPeer = Peer(network, network.checkpointBlock, myKey, node, this)
        blockHeaders.add(network.checkpointBlock)
    }

    //------------------Public methods----------------------

    fun start() {
        syncPeer.connect()
    }

    fun syncBlocks() {
        syncPeer.downloadBlocksFrom(blockHeaders.last())
    }

    //-----------------IPeerListener methods----------------

    override fun connected() {
        println("PeerGroup -> connected\n")
        syncBlocks()
    }

    override fun blocksReceived(blockHeaders: List<BlockHeader>) {
        println("PeerGroup -> blocksReceived\n")

        if (blockHeaders.size < 2) {
            println("blocks synced!\n")

            this.blockHeaders.lastOrNull()?.let { lastBlock ->
                syncPeer.getBalance(address, lastBlock.hashHex)
            }

            return
        }

        this.blockHeaders.addAll(blockHeaders)
        syncBlocks()
    }

    override fun proofReceived(message: ProofsMessage) {
        println("PeerGroup -> blocksReceived\n")

        val lastBlock = blockHeaders.last()

        try {
            val state = message.getValidatedState(lastBlock.stateRoot, address)
            println(state)
        } catch (ex: Exception) {
            ex.printStackTrace()
            println("proof error: $ex")
        }

    }
}