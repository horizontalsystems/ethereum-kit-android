package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.crypto.AESCipher
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import org.bouncycastle.crypto.digests.KeccakDigest
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.logging.Logger

class FrameCodec(private val secrets: Secrets,
                 private val frameCodecHelper: FrameCodecHelper = FrameCodecHelper(CryptoUtils),
                 private val enc: AESCipher = AESCipher(secrets.aes, true),
                 private val dec: AESCipher = AESCipher(secrets.aes, false)
) {

    private val logger = Logger.getLogger("FrameCodec")

    fun readFrame(inputStream: InputStream): Frame? {
        val headBuffer = ByteArray(32)
        inputStream.read(headBuffer)

        val header = headBuffer.copyOfRange(0, 16)
        val headerMac = headBuffer.copyOfRange(16, 32)
        val updatedMac = frameCodecHelper.updateMac(secrets.ingressMac, secrets.mac, header)

        if (!updatedMac.contentEquals(headerMac)) {
            throw FrameCodecError.HeaderMacMismatch()
        }

        val decryptedHeader = dec.process(header)

        val totalBodySize = frameCodecHelper.fromThreeBytes(decryptedHeader.copyOfRange(0, 3))
        val rlpList = RLP.decodeToOneItem(decryptedHeader, 3) as RLPList

        var contextId = -1
        var totalFrameSize = -1

        if (rlpList.size > 1) {
            contextId = RLP.decodeInt(rlpList[1])
            if (rlpList.size > 2) {
                totalFrameSize = RLP.decodeInt(rlpList[2])
            }
            if (contextId > 0) {
                logger.info("+++++++ Multi-frame message received $contextId $totalFrameSize")
            }
        }

        var paddingSize = 16 - totalBodySize % 16
        if (paddingSize == 16)
            paddingSize = 0
        val macSize = 16

        val bytesToRead = totalBodySize + paddingSize + macSize
        val buffer = ByteArray(bytesToRead) // body || padding || body-mac
        var bytesRead = 0

        while (bytesRead < bytesToRead) {
            bytesRead += inputStream.read(buffer, bytesRead, bytesToRead - bytesRead)
        }

        val frameSize = buffer.size - macSize

        val frameBodyData = buffer.copyOfRange(0, frameSize)
        val frameBodyMac = buffer.copyOfRange(frameSize, buffer.size)

        secrets.ingressMac.update(frameBodyData, 0, frameSize)
        val decryptedFrame = dec.process(frameBodyData)

        var pos = 0
        val type = RLP.decodeLong(decryptedFrame, pos)
        pos = RLP.getNextElementIndex(decryptedFrame, pos)
        val payload = decryptedFrame.copyOfRange(pos, totalBodySize)

        val ingressMac = ByteArray(secrets.ingressMac.digestSize)
        KeccakDigest(secrets.ingressMac).doFinal(ingressMac, 0)

        val updatedFrameBodyMac = frameCodecHelper.updateMac(secrets.ingressMac, secrets.mac, ingressMac)

        if (!updatedFrameBodyMac.contentEquals(frameBodyMac)) {
            throw FrameCodecError.BodyMacMismatch()
        }

        return Frame(type.toInt(), payload, totalFrameSize, contextId)
    }

    fun writeFrame(frame: Frame, outputStream: OutputStream) {
        val headBuffer = ByteArray(16)
        val packetType = RLP.encodeInt(frame.type)
        val frameSize = frame.size + packetType.size

        System.arraycopy(frameCodecHelper.toThreeBytes(frameSize), 0, headBuffer, 0, 3)

        val headerDataElements = ArrayList<ByteArray>()
        headerDataElements.add(RLP.encodeInt(0))
        if (frame.contextId >= 0)
            headerDataElements.add(RLP.encodeInt(frame.contextId))
        if (frame.totalFrameSize >= 0)
            headerDataElements.add(RLP.encodeInt(frame.totalFrameSize))

        val headerData = RLP.encodeList(*headerDataElements.toTypedArray())
        System.arraycopy(headerData, 0, headBuffer, 3, headerData.size)

        val encryptedHeader = enc.process(headBuffer)

        val headerMac = frameCodecHelper.updateMac(secrets.egressMac, secrets.mac, encryptedHeader)

        var frameData = packetType + frame.payload
        if (frameSize % 16 > 0) {
            frameData += ByteArray(16 - frameSize % 16)
        }

        val encryptedFrameData = enc.process(frameData)
        secrets.egressMac.update(encryptedFrameData, 0, encryptedFrameData.size)

        val egressMac = ByteArray(secrets.egressMac.digestSize)
        KeccakDigest(secrets.egressMac).doFinal(egressMac, 0)

        val frameMac = frameCodecHelper.updateMac(secrets.egressMac, secrets.mac, egressMac)

        outputStream.write(encryptedHeader + headerMac + encryptedFrameData + frameMac)
    }

    open class FrameCodecError : Exception() {
        class HeaderMacMismatch : FrameCodecError()
        class BodyMacMismatch : FrameCodecError()
    }
}
