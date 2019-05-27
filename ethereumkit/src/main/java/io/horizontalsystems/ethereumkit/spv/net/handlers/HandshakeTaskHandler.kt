package io.horizontalsystems.ethereumkit.spv.net.handlers

import io.horizontalsystems.ethereumkit.spv.core.*
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.les.LESPeer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage
import io.horizontalsystems.ethereumkit.spv.net.tasks.HandshakeTask

class HandshakeTaskHandler(private val listener: Listener? = null) : ITaskHandler, IMessageHandler {

    interface Listener {
        fun didCompleteHandshake(peer: IPeer, bestBlockHash: ByteArray, bestBlockHeight: Long)
    }

    private val tasks: MutableMap<String, HandshakeTask> = HashMap()

    override fun perform(task: ITask, requester: ITaskHandlerRequester): Boolean {
        if (task !is HandshakeTask) {
            return false
        }

        tasks[task.peerId] = task

        val message = StatusMessage(
                LESPeer.capability.version,
                task.networkId,
                task.genesisHash,
                task.headTotalDifficulty,
                task.headHash,
                task.headHeight
        )

        requester.send(message)

        return true
    }

    override fun handle(peer: IPeer, message: IInMessage): Boolean {
        if (message !is StatusMessage)
            return false


        val task = tasks[peer.id] ?: return false

        check(message.protocolVersion == LESPeer.capability.version) {
            throw LESPeer.InvalidProtocolVersion()
        }

        check(message.networkId == task.networkId) {
            throw LESPeer.WrongNetwork()
        }

        check(message.genesisHash.contentEquals(task.genesisHash)) {
            throw LESPeer.WrongNetwork()
        }

        check(message.headHeight >= task.headHeight) {
            throw LESPeer.ExpiredBestBlockHeight()
        }

        listener?.didCompleteHandshake(peer, message.headHash, message.headHeight)

        return true
    }
}
