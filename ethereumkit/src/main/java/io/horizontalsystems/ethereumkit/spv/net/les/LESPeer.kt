package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.*
import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.DevP2PPeer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.*
import java.util.logging.Logger

class LESPeer(private val devP2PPeer: DevP2PPeer) : IPeer, DevP2PPeer.Listener, ITaskHandlerRequester {

    private val logger = Logger.getLogger("LESPeer")

    private val taskHandlers: MutableList<ITaskHandler> = ArrayList()
    private val messageHandlers: MutableList<IMessageHandler> = ArrayList()

    //-------------IPeer methods----------------------

    override val id = devP2PPeer.myNodeId.toHexString()

    override var listener: IPeerListener? = null

    override fun register(messageHandler: IMessageHandler) {
        messageHandlers.add(messageHandler)
    }

    override fun register(taskHandler: ITaskHandler) {
        taskHandlers.add(taskHandler)
    }

    override fun connect() {
        devP2PPeer.connect()
    }

    override fun disconnect(error: Throwable?) {
        devP2PPeer.disconnect(error)
    }

    override fun add(task: ITask) {
        for (taskHandler in taskHandlers) {
            if (taskHandler.perform(task, this)) {
                return
            }
        }

        logger.info("No handler for task: ${task.javaClass.name}")
    }

    //-----------DevP2PPeer.Listener methods------------

    override fun didConnect() {
        logger.info("LESPeer -> didConnect\n")

        listener?.didConnect(this)
    }

    override fun didDisconnect(error: Throwable?) {
        logger.info("didDisconnect with error: ${error?.message}")
        listener?.didDisconnect(this, error)
    }

    override fun didReceive(message: IInMessage) {
        try {
            for (messageHandler in messageHandlers) {
                val handled = messageHandler.handle(this, message)
                if (handled)
                    return
            }
        } catch (error: Exception) {
            disconnect(error)
        }
    }

    //--------------ITaskHandlerRequester methods------------------

    override fun send(message: IOutMessage) {
        devP2PPeer.send(message)
    }

    companion object {
        val capability = Capability("les", 2,
                hashMapOf(0x00 to StatusMessage::class,
                        0x01 to AnnounceMessage::class,
                        0x02 to GetBlockHeadersMessage::class,
                        0x03 to BlockHeadersMessage::class,
                        0x0f to GetProofsMessage::class,
                        0x10 to ProofsMessage::class,
                        0x13 to SendTransactionMessage::class,
                        0x15 to TransactionStatusMessage::class))

        fun getInstance(key: ECKey, node: Node): LESPeer {
            val devP2PPeer = DevP2PPeer.getInstance(key, node, listOf(capability))
            val lesPeer = LESPeer(devP2PPeer)
            devP2PPeer.listener = lesPeer

            return lesPeer
        }

    }

    open class LESPeerError : Exception()

    open class LESPeerConsistencyError : Exception()
    class UnexpectedMessage : LESPeerConsistencyError()

    open class LESPeerValidationError : LESPeerError()
    class InvalidProtocolVersion : LESPeerValidationError()
    class WrongNetwork : LESPeerValidationError()
    class ExpiredBestBlockHeight : LESPeerValidationError()

}
