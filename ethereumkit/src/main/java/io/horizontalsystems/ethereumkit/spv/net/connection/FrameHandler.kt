package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.spv.net.ILESMessage
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.IP2PMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.BlockHeadersMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.ProofsMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage

class FrameHandler {

    companion object {
        const val P2P_MAX_MESSAGE_CODE = 0x0F
        const val LES_MAX_MESSAGE_CODE = 0x15 /*TxStatus*/
    }

    private val offsets: MutableMap<String, Int> = hashMapOf()
    private var frames: MutableList<Frame> = mutableListOf()
    private val capabilities: MutableList<Capability> = mutableListOf()

    fun addCapabilities(caps: List<Capability>) {
        capabilities.addAll(caps)

        var offset = P2P_MAX_MESSAGE_CODE + 1
        capabilities.sortedBy { it.name }.forEach { cap ->
            if (cap.name == Capability.LES) {
                offsets[Capability.LES] = offset
                offset += LES_MAX_MESSAGE_CODE + 1
            }
        }
    }

    fun addFrame(frame: Frame) {
        frames.add(frame)
    }

    fun getMessage(): IMessage? {
        val frame = frames.firstOrNull() ?: return null

        frames.remove(frame)

        var message: IMessage? = null

        try {
            if (frame.type in 0..P2P_MAX_MESSAGE_CODE) {
                message = when (frame.type) {
                    HelloMessage.code -> HelloMessage(frame.payload)
                    DisconnectMessage.code -> DisconnectMessage(frame.payload)
                    PingMessage.code -> PingMessage()
                    PongMessage.code -> PongMessage()
                    else -> null
                }
            }

            val lesOffset = offsets[Capability.LES]
            if (lesOffset != null && frame.type - lesOffset in 0..LES_MAX_MESSAGE_CODE) {
                message = when (frame.type - lesOffset) {
                    StatusMessage.code -> StatusMessage(frame.payload)
                    BlockHeadersMessage.code -> BlockHeadersMessage(frame.payload)
                    ProofsMessage.code -> ProofsMessage(frame.payload)
                    else -> null
                }
            }

        } catch (ex: Exception) {
            throw FrameHandlerError.InvalidPayload()
        }

        return message ?: throw FrameHandlerError.UnknownMessageType()
    }

    fun getFrames(message: IMessage): List<Frame> {
        val frames: MutableList<Frame> = mutableListOf()

        when (message) {
            is IP2PMessage -> frames.add(Frame(message.code, message.encoded()))
            is ILESMessage -> {
                val frameType = message.code + (offsets[Capability.LES] ?: 0)
                frames.add(Frame(frameType, message.encoded()))
            }
        }

        return frames
    }

    open class FrameHandlerError : Exception() {
        class UnknownMessageType : FrameHandlerError()
        class InvalidPayload : FrameHandlerError()
    }
}