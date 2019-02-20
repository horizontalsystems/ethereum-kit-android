package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.light.ByteUtils.xor
import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.light.rlp.RLP
import io.horizontalsystems.ethereumkit.light.rlp.RLP.rlpDecodeInt
import io.horizontalsystems.ethereumkit.light.rlp.RLPList
import org.spongycastle.crypto.StreamCipher
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class FrameCodec(private val secrets: Secrets) {

    private val enc: StreamCipher
    private val dec: StreamCipher

    init {
        val encAesEngine = AESEngine()
        enc = SICBlockCipher(encAesEngine)
        enc.init(true, ParametersWithIV(KeyParameter(secrets.aes), ByteArray(encAesEngine.blockSize)))

        val decAesEngine = AESEngine()
        dec = SICBlockCipher(decAesEngine)
        dec.init(false, ParametersWithIV(KeyParameter(secrets.aes), ByteArray(decAesEngine.blockSize)))
    }

    fun readFrame(inputStream: InputStream): Frame? {
        val headBuffer = ByteArray(32)
        inputStream.read(headBuffer)

        val header = headBuffer.copyOfRange(0, 16)
        val headerMac = headBuffer.copyOfRange(16, 32)
        val updatedMac = updateMac(secrets.ingressMac, secrets.mac, header)

        if (!updatedMac.contentEquals(headerMac)) {
            println("Frame Header MAC mismatch!")
            return null
        }

        dec.processBytes(headBuffer, 0, 16, headBuffer, 0)
        var totalBodySize = headBuffer[0].toInt() and 0xFF
        totalBodySize = (totalBodySize shl 8) + (headBuffer[1].toInt() and 0xFF)
        totalBodySize = (totalBodySize shl 8) + (headBuffer[2].toInt() and 0xFF)

        val rlpList = RLP.decode2OneItem(headBuffer, 3) as RLPList

        val protocol = RLP.rlpDecodeInt(rlpList[0])
        val contextId: Int
        var totalFrameSize = -1

        if (rlpList.size > 1) {
            contextId = rlpDecodeInt(rlpList[1])
            if (rlpList.size > 2) {
                totalFrameSize = rlpDecodeInt(rlpList[2])
            }
            if (contextId > 0) {
                println("+++++++ Multi-frame message received $contextId $totalFrameSize")
            }
        }

        var paddingSize = 16 - totalBodySize % 16
        if (paddingSize == 16)
            paddingSize = 0
        val macSize = 16

        val buffer = ByteArray(totalBodySize + paddingSize + macSize) // body || padding || body-mac

        inputStream.read(buffer)

        val frameSize = buffer.size - macSize

        val frameBodyData = buffer.copyOfRange(0, frameSize)
        val frameBodyMac = buffer.copyOfRange(frameSize, buffer.size)

        secrets.ingressMac.update(frameBodyData, 0, frameSize)

        val decryptedFrame = ByteArray(frameSize)
        dec.processBytes(frameBodyData, 0, frameSize, decryptedFrame, 0)

        var pos = 0
        val type = RLP.decodeLong(decryptedFrame, pos)

        pos = RLP.getNextElementIndex(decryptedFrame, pos)
        val payload = decryptedFrame.copyOfRange(pos, totalBodySize)

        val ingressMac = ByteArray(secrets.ingressMac.digestSize)
        KeccakDigest(secrets.ingressMac).doFinal(ingressMac, 0)

        val updatedFrameBodyMac = updateMac(secrets.ingressMac, secrets.mac, ingressMac)

        if (!updatedFrameBodyMac.contentEquals(frameBodyMac)) {
            println("Frame Body MAC mismatch!")
            return null
        }

        return Frame(type.toInt(), payload)
    }

    fun writeFrame(frame: Frame, outputStream: OutputStream) {

        val headBuffer = ByteArray(32)
        val packetType = RLP.encodeInt(frame.type)
        val frameSize = frame.size + packetType.size
        headBuffer[0] = (frameSize shr 16).toByte()
        headBuffer[1] = (frameSize shr 8).toByte()
        headBuffer[2] = frameSize.toByte()

        val headerDataElements = ArrayList<ByteArray>()
        headerDataElements.add(RLP.encodeInt(0))
        if (frame.contextId >= 0)
            headerDataElements.add(RLP.encodeInt(frame.contextId))
        if (frame.totalFrameSize >= 0)
            headerDataElements.add(RLP.encodeInt(frame.totalFrameSize))

        val headerData = RLP.encodeList(*headerDataElements.toTypedArray())
        System.arraycopy(headerData, 0, headBuffer, 3, headerData.size)

        enc.processBytes(headBuffer, 0, 16, headBuffer, 0)

        val headerMac = updateMac(secrets.egressMac, secrets.mac, headBuffer)

        val header = headBuffer.copyOfRange(0, 16) + headerMac //encrypted header + headerMac

        var frameData = packetType + frame.payload
        if (frameSize % 16 > 0) {
            frameData += ByteArray(16 - frameSize % 16)
        }

        val encryptedFrameData = ByteArray(frameData.size)
        enc.processBytes(frameData, 0, frameData.size, encryptedFrameData, 0)

        secrets.egressMac.update(encryptedFrameData, 0, encryptedFrameData.size)

        val egressMac = ByteArray(secrets.egressMac.digestSize)
        KeccakDigest(secrets.egressMac).doFinal(egressMac, 0)

        val frameMac = updateMac(secrets.egressMac, secrets.mac, egressMac)

        frameData = encryptedFrameData + frameMac

        outputStream.write(header)
        outputStream.write(frameData)
    }

    private fun updateMac(mac: KeccakDigest, macKey: ByteArray, data: ByteArray): ByteArray {
        val macDigest = ByteArray(mac.digestSize)
        KeccakDigest(mac).doFinal(macDigest, 0)

        val encryptedMacDigest = CryptoUtils.encryptAES(macKey, macDigest)

        mac.update(xor(encryptedMacDigest, data), 0, 16)
        KeccakDigest(mac).doFinal(macDigest, 0)

        return macDigest.copyOfRange(0, 16) //checksum
    }
}
