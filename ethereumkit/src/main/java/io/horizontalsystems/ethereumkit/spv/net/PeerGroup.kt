package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.spv.core.*

class PeerGroup(private val peerProvider: PeerProvider) : IPeer, IPeerListener {

    private val peers: MutableList<IPeer> = ArrayList()

    init {
        val peer = peerProvider.getPeer()
        peer.listener = this
        peers.add(peer)
    }

    //-----------------IPeer-------------------

    override val id: String
        get() = TODO("not implemented")

    override var listener: IPeerListener? = null

    override fun register(messageHandler: IMessageHandler) {
        peers.forEach { peer ->
            peer.register(messageHandler)
        }
    }

    override fun connect() {
        peers.forEach { peer ->
            peer.connect()
        }
    }

    override fun disconnect(error: Throwable?) {
        peers.forEach { peer ->
            peer.disconnect(error)
        }
    }

    override fun register(taskHandler: ITaskHandler) {
        peers.forEach { peer ->
            peer.register(taskHandler)
        }
    }

    override fun add(task: ITask) {
        peers.firstOrNull()?.add(task)
    }

//-----------------IPeerListener------------------

    override fun didConnect(peer: IPeer) {
        listener?.didConnect(peer)
    }

    override fun didDisconnect(peer: IPeer, error: Throwable?) {
        listener?.didDisconnect(peer, error)
    }

}
