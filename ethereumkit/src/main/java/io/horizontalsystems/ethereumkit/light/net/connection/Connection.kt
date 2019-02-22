package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.light.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.net.IMessage
import io.horizontalsystems.ethereumkit.light.net.Node
import io.horizontalsystems.ethereumkit.light.toShort
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


interface IPeerConnectionListener {
    fun connectionKey(): ECKey
    fun onConnectionEstablished()
    fun onDisconnected(error: Throwable?)
    fun onMessageReceived(message: IMessage)
}

interface IPeerConnection {
    val listener: IPeerConnectionListener
    val logName: String
    fun connect()
    fun disconnect(error: Throwable?)
    fun send(message: IMessage)
}

class Connection(private val node: Node, override val listener: IPeerConnectionListener) : IPeerConnection, Thread() {
    override val logName: String = "${node.id}@${node.host}:${node.port}"

    private val logger = Logger.getLogger("Peer[${node.host}]")
    private val sendingQueue: BlockingQueue<IMessage> = ArrayBlockingQueue(100)
    private val socket = Socket()

    @Volatile
    private var isRunning = false

    var connected = false
    var handshakeSent = false

    private lateinit var handshake: EncryptionHandshake
    private lateinit var frameCodec: FrameCodec

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

    override fun connect() {
        start()
    }

    override fun disconnect(error: Throwable?) {
        TODO("not implemented")
    }

    override fun send(message: IMessage) {
        println(">>>>> $message\n")

        sendingQueue.put(message)
    }

    private fun initiateHandshake(outputStream: OutputStream) {
        if (handshakeSent)
            return
        handshakeSent = true

        handshake = EncryptionHandshake(listener.connectionKey(), remotePublicKeyPoint)
        handshake.createAuthMessage()

        outputStream.write(handshake.authMessagePacket)
    }

    private fun handleAuthAckMessage(inputsStream: InputStream): Secrets {
        val prefixBytes = ByteArray(2)
        inputsStream.read(prefixBytes)

        val size = prefixBytes.toShort()
        val messagePackets = ByteArray(size.toInt())

        inputsStream.read(messagePackets)

        return handshake.handleAuthAckMessage(ECIESEncryptedMessage.decode(prefixBytes + messagePackets))
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

            listener.onConnectionEstablished()

            while (isRunning) {

                val msg = sendingQueue.poll(1, TimeUnit.SECONDS)
                if (isRunning && msg != null) {
                    val frame = Frame(msg)
                    frameCodec.writeFrame(frame, outputStream)
                }

                while (isRunning && inputStream.available() > 0) {

                    val frame = frameCodec.readFrame(inputStream)
                    if (frame == null) {
                        println("Frame is NULL")
                    } else {

                        val message = Frame.frameToMessage(frame)
                        if (message == null) {
                            println("Message is NULL")
                        } else {
                            listener.onMessageReceived(message)
                        }
                    }
                }
            }

        } catch (e: SocketTimeoutException) {
            logger.warning("Socket timeout exception: ${e.message}")
            listener.onDisconnected(e)
        } catch (e: ConnectException) {
            logger.warning("Connect exception: ${e.message}")
            listener.onDisconnected(e)
        } catch (e: IOException) {
            logger.warning("IOException: ${e.message}")
            listener.onDisconnected(e)
        } catch (e: InterruptedException) {
            logger.warning("Peer connection thread interrupted: ${e.message}")
            listener.onDisconnected(e)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.warning("Peer connection exception: ${e.message}")
            listener.onDisconnected(e)
        } finally {
            isRunning = false

            inputStream.close()
            outputStream.close()

            socket.close()
        }
    }
}