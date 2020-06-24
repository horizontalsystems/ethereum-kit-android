package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.spv.core.toShort
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import io.horizontalsystems.ethereumkit.spv.net.Node
import org.bouncycastle.math.ec.ECPoint
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


class Connection(private val connectionKey: ECKey, private val node: Node) : Thread() {
    interface Listener {
        fun didConnect()
        fun didDisconnect(error: Throwable?)
        fun didReceive(frame: Frame)
    }

    private val logger = Logger.getLogger("Connection")

    var listener: Listener? = null

    private val sendingQueue: BlockingQueue<Frame> = ArrayBlockingQueue(100)
    private val socket = Socket()

    @Volatile
    private var isRunning = false

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

    fun connect() {
        start()
    }

    fun disconnect(error: Throwable?) {
        logger.info("disconnect with error: ${error?.message}")
        isRunning = false
    }

    fun send(frame: Frame) {
        sendingQueue.put(frame)
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

            logger.info("Socket ${node.host} didConnect.")

            initiateHandshake(outputStream)

            val secrets = handleAuthAckMessage(inputStream)

            frameCodec = FrameCodec(secrets)

            listener?.didConnect()

            while (isRunning) {

                val frameToSend = sendingQueue.poll(1, TimeUnit.SECONDS)
                if (isRunning && frameToSend != null) {
                    frameCodec.writeFrame(frameToSend, outputStream)
                }

                if (isRunning && inputStream.available() > 0) {
                    val frameReceived = frameCodec.readFrame(inputStream)

                    if (frameReceived == null) {
                        logger.info("Frame is NULL")
                    } else {
                        listener?.didReceive(frameReceived)
                    }
                }
            }

        } catch (e: SocketTimeoutException) {
            logger.warning("Connection error: ${e.message}")
            listener?.didDisconnect(e)
        } catch (e: ConnectException) {
            logger.warning("Connection error: ${e.message}")
            listener?.didDisconnect(e)
        } catch (e: IOException) {
            logger.warning("Connection error: ${e.message}")
            listener?.didDisconnect(e)
        } catch (e: InterruptedException) {
            logger.warning("Connection error: ${e.message}")
            listener?.didDisconnect(e)
        } catch (e: Exception) {
            logger.warning("Connection error: ${e.message}")
            listener?.didDisconnect(e)
        } finally {
            isRunning = false

            inputStream.close()
            outputStream.close()

            socket.close()
        }
    }
}