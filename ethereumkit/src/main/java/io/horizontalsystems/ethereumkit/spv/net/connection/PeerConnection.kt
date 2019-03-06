package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.spv.core.toShort
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.spv.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import org.spongycastle.math.ec.ECPoint
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Logger


class PeerConnection(private val connectionKey: ECKey, private val node: Node) : Thread() {
    interface Listener {
        fun onConnectionEstablished()
        fun onDisconnected(error: Throwable?)
        fun onMessageReceived(message: IMessage)
    }

    var listener: Listener? = null

    private val logName: String = "${node.id}@${node.host}:${node.port}"
    private val logger = Logger.getLogger("Peer[${node.host}]")
    private val sendingQueue: BlockingQueue<IMessage> = ArrayBlockingQueue(100)
    private val socket = Socket()

    @Volatile
    private var isRunning = false

    private lateinit var handshake: EncryptionHandshake
    private lateinit var frameCodec: FrameCodec
    private val frameHandler: FrameHandler = FrameHandler()

    private val remotePublicKeyPoint: ECPoint
            by lazy {
                val remotePublicKey = ByteArray(65)
                val nodeId = this.node.id
                System.arraycopy(nodeId, 0, remotePublicKey, 1, nodeId.size)
                remotePublicKey[0] = 0x04
                CURVE.curve.decodePoint(remotePublicKey)
            }

    init {
        isDaemon = true
    }

    fun connect() {
        start()
    }

    fun disconnect(error: Throwable?) {
        println("disconnect with error: $error")
        isRunning = false
    }

    fun send(message: IMessage) {
        println(">>>>> $message\n")

        sendingQueue.put(message)
    }

    fun register(capabilities: List<Capability>) {
        frameHandler.register(capabilities)
    }

    private fun initiateHandshake(outputStream: OutputStream) {
        handshake = EncryptionHandshake(connectionKey, remotePublicKeyPoint, CryptoUtils, RandomHelper)

        val authMessagePackets = handshake.createAuthMessage()

        outputStream.write(authMessagePackets)
    }

    private fun handleAuthAckMessage(inputsStream: InputStream): Secrets {
        val prefixBytes = ByteArray(2)
        inputsStream.read(prefixBytes)

        val size = prefixBytes.toShort()
        val messagePackets = ByteArray(size.toInt())

        inputsStream.read(messagePackets)

        return handshake.handleAuthAckMessage(ECIESEncryptedMessage(prefixBytes + messagePackets))
    }

    override fun run() {
        isRunning = true
        // connect:
        socket.connect(InetSocketAddress(node.host, node.port), 10000)
        socket.soTimeout = 10000

        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()

        try {

            logger.info("Socket ${node.host} connected.")

            initiateHandshake(outputStream)

            val secrets = handleAuthAckMessage(inputStream)

            frameCodec = FrameCodec(secrets)

            listener?.onConnectionEstablished()

            while (isRunning) {

                val msg = sendingQueue.poll(1, TimeUnit.SECONDS)
                if (isRunning && msg != null) {
                    frameHandler.getFrames(msg).forEach { frame ->
                        frameCodec.writeFrame(frame, outputStream)
                    }
                }

                while (isRunning && inputStream.available() > 0) {
                    val frame = frameCodec.readFrame(inputStream)
                    if (frame == null) {
                        println("Frame is NULL")
                    } else {
                        frameHandler.addFrame(frame)

                        frameHandler.getMessage()?.let { message ->
                            listener?.onMessageReceived(message)
                        }
                    }
                }
            }

        } catch (e: SocketTimeoutException) {
            logger.warning("Socket timeout exception: ${e.message}")
            listener?.onDisconnected(e)
        } catch (e: ConnectException) {
            logger.warning("Connect exception: ${e.message}")
            listener?.onDisconnected(e)
        } catch (e: IOException) {
            logger.warning("IOException: ${e.message}")
            listener?.onDisconnected(e)
        } catch (e: InterruptedException) {
            logger.warning("Peer connection thread interrupted: ${e.message}")
            listener?.onDisconnected(e)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.warning("Peer connection exception: ${e.message}")
            listener?.onDisconnected(e)
        } finally {
            isRunning = false

            inputStream.close()
            outputStream.close()

            socket.close()
        }
    }
}