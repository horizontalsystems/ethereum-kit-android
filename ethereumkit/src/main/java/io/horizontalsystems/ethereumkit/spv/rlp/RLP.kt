package io.horizontalsystems.ethereumkit.spv.rlp

import io.horizontalsystems.ethereumkit.core.toByteArray
import io.horizontalsystems.ethereumkit.spv.core.toBytesNoLeadZeroes
import io.horizontalsystems.ethereumkit.spv.core.toInt
import org.bouncycastle.util.Arrays.concatenate
import org.bouncycastle.util.BigIntegers.asUnsignedByteArray
import org.bouncycastle.util.encoders.Hex
import java.io.Serializable
import java.math.BigInteger
import java.util.*
import kotlin.experimental.and
import kotlin.math.pow

interface RLPElement : Serializable {
    val rlpData: ByteArray?
}

class RLPItem(initValue: ByteArray?) : RLPElement {
    override val rlpData: ByteArray? = initValue
        get() = if (field?.isNotEmpty() == true) field else null
}

class RLPList : ArrayList<RLPElement>(), RLPElement {
    override var rlpData: ByteArray? = null

    fun valueElement(name: String): RLPElement? {
        for (rlpElement in this) {
            if (rlpElement is RLPList && String(rlpElement[0].rlpData ?: byteArrayOf()) == name) {
                return rlpElement[1]
            }
        }
        return null
    }
}

object RLP {
    private const val OFFSET_SHORT_ITEM = 0x80
    private const val OFFSET_LONG_ITEM = 0xb7
    private const val OFFSET_SHORT_LIST = 0xc0
    private const val OFFSET_LONG_LIST = 0xf7
    private const val SIZE_THRESHOLD = 56

    private const val MAX_DEPTH = 16
    private val MAX_ITEM_LENGTH = 256.0.pow(8.0)

    fun encode(input: Any): ByteArray {
        val value = Value(input)
        if (value.isList()) {
            val inputArray = value.asList()
            if (inputArray.isEmpty()) {
                return encodeLength(inputArray.size, OFFSET_SHORT_LIST)
            }
            var output = byteArrayOf()
            for (any in inputArray) {
                output = concatenate(output, encode(any))
            }
            val prefix = encodeLength(output.size, OFFSET_SHORT_LIST)
            return concatenate(prefix, output)
        } else {
            val inputAsBytes = toBytes(input)
            return if (inputAsBytes.size == 1 && (inputAsBytes[0].toInt() and 0xFF) <= 0x80) {
                inputAsBytes
            } else {
                val firstByte = encodeLength(inputAsBytes.size, OFFSET_SHORT_ITEM)
                concatenate(firstByte, inputAsBytes)
            }
        }
    }

    fun encodeInt(singleInt: Int) = when (singleInt) {
        singleInt and 0xFF -> encodeByte(singleInt.toByte())
        singleInt and 0xFFFF -> encodeShort(singleInt.toShort())
        singleInt and 0xFFFFFF -> byteArrayOf((OFFSET_SHORT_ITEM + 3).toByte(), singleInt.ushr(16).toByte(), singleInt.ushr(8).toByte(), singleInt.toByte())
        else -> byteArrayOf((OFFSET_SHORT_ITEM + 4).toByte(), singleInt.ushr(24).toByte(), singleInt.ushr(16).toByte(), singleInt.ushr(8).toByte(), singleInt.toByte())
    }

    fun encodeLong(longValue: Long): ByteArray {
        return encodeElement(longValue.toByteArray())
    }

    fun encodeByte(singleByte: Byte) = when {
        (singleByte.toInt() and 0xFF) == 0 -> byteArrayOf(OFFSET_SHORT_ITEM.toByte())
        (singleByte.toInt() and 0xFF) <= 0x7F -> byteArrayOf(singleByte)
        else -> byteArrayOf((OFFSET_SHORT_ITEM + 1).toByte(), singleByte)
    }

    fun encodeString(srcString: String?): ByteArray {
        return encodeElement(srcString?.toByteArray())
    }

    fun encodeBigInteger(srcBigInteger: BigInteger): ByteArray {
        if (srcBigInteger < BigInteger.ZERO)
            throw RuntimeException("negative numbers are not allowed")

        return if (srcBigInteger == BigInteger.ZERO)
            encodeByte(0.toByte())
        else
            encodeElement(asUnsignedByteArray(srcBigInteger))
    }

    fun encodeElement(srcData: ByteArray?): ByteArray {

        // [0x80]
        if (srcData == null || srcData.isEmpty()) {
            return byteArrayOf(OFFSET_SHORT_ITEM.toByte())

            // [0x00]
        } else if (srcData.size == 1 && srcData[0].toInt() == 0) {
            return srcData

            // [0x01, 0x7f] - single byte, that byte is its own RLP encoding
        } else if (srcData.size == 1 && srcData[0].toInt() and 0xFF < 0x80) {
            return srcData

            // [0x80, 0xb7], 0 - 55 bytes
        } else if (srcData.size < SIZE_THRESHOLD) {
            // length = 8X
            val length = (OFFSET_SHORT_ITEM + srcData.size).toByte()
            val data = Arrays.copyOf(srcData, srcData.size + 1)
            System.arraycopy(data, 0, data, 1, srcData.size)
            data[0] = length

            return data
            // [0xb8, 0xbf], 56+ bytes
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            var tmpLength = srcData.size
            var lengthOfLength: Byte = 0
            while (tmpLength != 0) {
                ++lengthOfLength
                tmpLength = tmpLength shr 8
            }

            // set length Of length at first byte
            val data = ByteArray(1 + lengthOfLength.toInt() + srcData.size)
            data[0] = (OFFSET_LONG_ITEM + lengthOfLength).toByte()

            // copy length after first byte
            tmpLength = srcData.size
            for (i in lengthOfLength downTo 1) {
                data[i] = (tmpLength and 0xFF).toByte()
                tmpLength = tmpLength shr 8
            }

            // at last copy the number bytes after its length
            System.arraycopy(srcData, 0, data, 1 + lengthOfLength, srcData.size)

            return data
        }
    }

    private fun encodeShort(singleShort: Short) =
            if ((singleShort and 0xFF) == singleShort)
                encodeByte(singleShort.toByte())
            else {
                byteArrayOf((OFFSET_SHORT_ITEM + 2).toByte(), (singleShort.toInt() shr 8 and 0xFF).toByte(), (singleShort.toInt() shr 0 and 0xFF).toByte())
            }

    fun encodeList(vararg elements: ByteArray): ByteArray {
        var totalLength = 0
        for (element1 in elements) {
            totalLength += element1.size
        }

        val data: ByteArray
        var copyPos: Int
        if (totalLength < SIZE_THRESHOLD) {

            data = ByteArray(1 + totalLength)
            data[0] = (OFFSET_SHORT_LIST + totalLength).toByte()
            copyPos = 1
        } else {
            var tmpLength = totalLength
            var byteNum: Byte = 0
            while (tmpLength != 0) {
                ++byteNum
                tmpLength = tmpLength shr 8
            }
            tmpLength = totalLength
            val lenBytes = ByteArray(byteNum.toInt())
            for (i in 0 until byteNum) {
                lenBytes[byteNum.toInt() - 1 - i] = (tmpLength shr 8 * i and 0xFF).toByte()
            }
            data = ByteArray(1 + lenBytes.size + totalLength)
            data[0] = (OFFSET_LONG_LIST + byteNum).toByte()
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.size)

            copyPos = lenBytes.size + 1
        }
        for (element in elements) {
            System.arraycopy(element, 0, data, copyPos, element.size)
            copyPos += element.size
        }
        return data
    }

    fun decode(data: ByteArray, posParam: Int): DecodeResult {
        var pos = posParam
        val prefix = data[pos].toInt() and 0xFF

        when {
            prefix == OFFSET_SHORT_ITEM -> return DecodeResult(pos + 1, "")
            prefix < OFFSET_SHORT_ITEM -> return DecodeResult(pos + 1, byteArrayOf(data[pos]))
            prefix <= OFFSET_LONG_ITEM -> {
                val len = prefix - OFFSET_SHORT_ITEM
                return DecodeResult(pos + 1 + len, data.copyOfRange(pos + 1, pos + 1 + len))
            }
            prefix < OFFSET_SHORT_LIST -> {
                val lenlen = prefix - OFFSET_LONG_ITEM
                val lenbytes = data.copyOfRange(pos + 1, pos + 1 + lenlen).toInt()

                return DecodeResult(pos + 1 + lenlen + lenbytes, data.copyOfRange(pos + 1 + lenlen, pos + 1 + lenlen
                        + lenbytes))
            }
            prefix <= OFFSET_LONG_LIST -> {
                val len = prefix - OFFSET_SHORT_LIST
                pos++
                return decodeList(data, pos, len)
            }
            prefix <= 0xFF -> {
                val lenlen = prefix - OFFSET_LONG_LIST
                val lenlist = data.copyOfRange(pos + 1, pos + 1 + lenlen).toInt()
                pos += lenlen + 1
                return decodeList(data, pos, lenlist)
            }
            else -> throw RuntimeException("Only byte values between 0x00 and 0xFF are supported, but got: $prefix")
        }
    }

    fun decode2(msgData: ByteArray): RLPList {
        val rlpList = RLPList()
        fullTraverse(msgData, 0, 0, msgData.size, rlpList, Integer.MAX_VALUE)
        return rlpList
    }

    private fun decodeList(data: ByteArray, posParam: Int, len: Int): DecodeResult {
        var pos = posParam
        var prevPos: Int

        val slice = ArrayList<Any>()
        var i = 0
        while (i < len) {
            val result = decode(data, pos)
            slice.add(result.decoded)
            prevPos = result.pos
            i += prevPos - pos
            pos = prevPos
        }
        return DecodeResult(pos, slice.toTypedArray())
    }

    private fun encodeLength(length: Int, offset: Int) = when {
        length < SIZE_THRESHOLD -> {
            val firstByte = (length + offset).toByte()
            byteArrayOf(firstByte)
        }
        length < MAX_ITEM_LENGTH -> {
            val binaryLength = if (length > 0xFF)
                length.toBytesNoLeadZeroes()
            else
                byteArrayOf(length.toByte())
            val firstByte = (binaryLength.size + offset + SIZE_THRESHOLD - 1).toByte()
            concatenate(byteArrayOf(firstByte), binaryLength)
        }
        else -> throw RuntimeException("Input too long")
    }

    private fun toBytes(input: Any?): ByteArray = when (input) {
        is ByteArray -> input
        is String -> input.toByteArray()
        is Long -> if (input == 0) byteArrayOf() else asUnsignedByteArray(BigInteger.valueOf(input))
        is Int -> if (input == 0) byteArrayOf() else asUnsignedByteArray(BigInteger.valueOf(input.toLong()))
        is BigInteger -> if (input == BigInteger.ZERO) byteArrayOf() else asUnsignedByteArray(input)
        is Value -> toBytes(input.asObj())
        else -> throw RuntimeException("Unsupported type: Only accepting String, Integer and BigInteger for now")
    }

    fun fullTraverse(msgData: ByteArray?, level: Int, startPos: Int,
                     endPos: Int, rlpList: RLPList, depth: Int) {
        if (level > MAX_DEPTH) {
            throw RuntimeException(String.format("Error: Traversing over max RLP depth (%s)", MAX_DEPTH))
        }

        try {
            if (msgData == null || msgData.isEmpty())
                return
            var pos = startPos

            while (pos < endPos) {

                // It's a list with a payload more than 55 bytes
                // data[0] - 0xF7 = how many next bytes allocated
                // for the length of the list
                if (msgData[pos].toInt() and 0xFF > OFFSET_LONG_LIST) {

                    val lengthOfLength = ((msgData[pos].toInt() and 0xFF) - OFFSET_LONG_LIST).toByte()
                    val length = calcLength(lengthOfLength.toInt(), msgData, pos)

                    if (length < SIZE_THRESHOLD) {
                        throw RuntimeException("Short list has been encoded as long list")
                    }

                    val rlpData = ByteArray(lengthOfLength.toInt() + length + 1)
                    System.arraycopy(msgData, pos, rlpData, 0, lengthOfLength.toInt()
                            + length + 1)

                    if (level + 1 < depth) {
                        val newLevelList = RLPList()
                        newLevelList.rlpData = rlpData

                        fullTraverse(msgData, level + 1, pos + lengthOfLength.toInt() + 1,
                                pos + lengthOfLength.toInt() + length + 1, newLevelList, depth)
                        rlpList.add(newLevelList)
                    } else {
                        rlpList.add(RLPItem(rlpData))
                    }

                    pos += lengthOfLength.toInt() + length + 1
                    continue
                }
                // It's a list with a payload less than 55 bytes
                if (msgData[pos].toInt() and 0xFF in OFFSET_SHORT_LIST..OFFSET_LONG_LIST) {

                    val length = ((msgData[pos].toInt() and 0xFF) - OFFSET_SHORT_LIST).toByte()

                    val rlpData = ByteArray(length + 1)
                    System.arraycopy(msgData, pos, rlpData, 0, length + 1)

                    if (level + 1 < depth) {
                        val newLevelList = RLPList()
                        newLevelList.rlpData = rlpData

                        if (length > 0)
                            fullTraverse(msgData, level + 1, pos + 1, pos + length.toInt() + 1, newLevelList, depth)
                        rlpList.add(newLevelList)
                    } else {
                        rlpList.add(RLPItem(rlpData))
                    }

                    pos += 1 + length
                    continue
                }
                // It's an item with a payload more than 55 bytes
                // data[0] - 0xB7 = how much next bytes allocated for
                // the length of the string
                if (msgData[pos].toInt() and 0xFF in (OFFSET_LONG_ITEM + 1) until OFFSET_SHORT_LIST) {

                    val lengthOfLength = (msgData[pos].toInt() and 0xFF) - OFFSET_LONG_ITEM
                    val length = calcLength(lengthOfLength, msgData, pos)
                    if (length < SIZE_THRESHOLD) {
                        throw RuntimeException("Short item has been encoded as long item")
                    }

                    // now we can parse an item for data[1]..data[length]
                    val item = ByteArray(length)
                    System.arraycopy(msgData, pos + lengthOfLength + 1, item,
                            0, length)

                    val rlpItem = RLPItem(item)
                    rlpList.add(rlpItem)
                    pos += lengthOfLength + length + 1

                    continue
                }
                // It's an item less than 55 bytes long,
                // data[0] - 0x80 == length of the item
                if (msgData[pos].toInt() and 0xFF in (OFFSET_SHORT_ITEM + 1)..OFFSET_LONG_ITEM) {

                    val length = (msgData[pos].toInt() and 0xFF) - OFFSET_SHORT_ITEM

                    val item = ByteArray(length)
                    System.arraycopy(msgData, pos + 1, item, 0, length)

                    if (length == 1 && item[0].toInt() and 0xFF < OFFSET_SHORT_ITEM) {
                        throw RuntimeException("Single byte has been encoded as byte string")
                    }

                    val rlpItem = RLPItem(item)
                    rlpList.add(rlpItem)
                    pos += 1 + length

                    continue
                }
                // null item
                if ((msgData[pos].toInt() and 0xFF) == OFFSET_SHORT_ITEM) {
                    val item = byteArrayOf()
                    val rlpItem = RLPItem(item)
                    rlpList.add(rlpItem)
                    pos += 1
                    continue
                }
                // single byte item
                if (msgData[pos].toInt() and 0xFF < OFFSET_SHORT_ITEM) {

                    val item = byteArrayOf((msgData[pos].toInt() and 0xFF).toByte())

                    val rlpItem = RLPItem(item)
                    rlpList.add(rlpItem)
                    pos += 1
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("RLP wrong encoding (" + Hex.toHexString(msgData, startPos, endPos - startPos) + ")", e)
        } catch (e: OutOfMemoryError) {
            throw RuntimeException("Invalid RLP (excessive mem allocation while parsing) (" + Hex.toHexString(msgData, startPos, endPos - startPos) + ")", e)
        }

    }

    private fun calcLength(lengthOfLength: Int, msgData: ByteArray, pos: Int): Int {
        var pow = (lengthOfLength - 1).toByte()
        var length = 0
        for (i in 1..lengthOfLength) {

            val bt = msgData[pos + i].toInt() and 0xFF
            val shift = 8 * pow

            // no leading zeros are acceptable
            if (bt == 0 && length == 0) {
                throw RuntimeException("RLP length contains leading zeros")
            }

            // return MAX_VALUE if index of highest bit is more than 31
            if (32 - Integer.numberOfLeadingZeros(bt) + shift > 31) {
                return Integer.MAX_VALUE
            }

            length += bt shl shift
            pow--
        }

        return length
    }

    fun decodeToOneItem(msgData: ByteArray, startPos: Int): RLPElement {
        val rlpList = RLPList()
        fullTraverse(msgData, 0, startPos, startPos + 1, rlpList, Integer.MAX_VALUE)
        return rlpList[0]
    }

    fun decodeInt(elem: RLPElement): Int {
        val b = elem.rlpData
        return b.toInt()
    }

    fun decodeLong(data: ByteArray, index: Int): Long {
        var value: Long = 0
        when {
            data[index].toInt() == 0x00 -> throw RuntimeException("not a number")
            data[index].toInt() and 0xFF < OFFSET_SHORT_ITEM -> return data[index].toLong()
            data[index].toInt() and 0xFF <= OFFSET_SHORT_ITEM + java.lang.Long.BYTES -> {

                val length = ((data[index].toInt() and 0xFF) - OFFSET_SHORT_ITEM)
                var pow = (length - 1).toByte()
                for (i in 1..length) {
                    // << (8 * pow) == bit shift to 0 (*1), 8 (*256) , 16 (*65..)..
                    value += (data[index + i].toInt() and 0xFF).toLong() shl 8 * pow
                    pow--
                }
            }
            else -> // If there are more than 8 bytes, it is not going
                // to decode properly into a long.
                throw RuntimeException("wrong decode attempt")
        }
        return value
    }

    fun getNextElementIndex(payload: ByteArray, pos: Int): Int {
        if (pos >= payload.size)
            return -1

        // [0xf8, 0xff]
        if (payload[pos].toInt() and 0xFF > OFFSET_LONG_LIST) {
            val lengthOfLength = ((payload[pos].toInt() and 0xFF) - OFFSET_LONG_LIST).toByte()
            val length = calcLength(lengthOfLength.toInt(), payload, pos)
            return pos + lengthOfLength.toInt() + length + 1
        }
        // [0xc0, 0xf7]
        if (payload[pos].toInt() and 0xFF in OFFSET_SHORT_LIST..OFFSET_LONG_LIST) {

            val length = ((payload[pos].toInt() and 0xFF) - OFFSET_SHORT_LIST).toByte()
            return pos + 1 + length.toInt()
        }
        // [0xb8, 0xbf]
        if (payload[pos].toInt() and 0xFF in (OFFSET_LONG_ITEM + 1) until OFFSET_SHORT_LIST) {

            val lengthOfLength = ((payload[pos].toInt() and 0xFF) - OFFSET_LONG_ITEM).toByte()
            val length = calcLength(lengthOfLength.toInt(), payload, pos)
            return pos + lengthOfLength.toInt() + length + 1
        }
        // [0x81, 0xb7]
        if (payload[pos].toInt() and 0xFF in (OFFSET_SHORT_ITEM + 1)..OFFSET_LONG_ITEM) {

            val length = ((payload[pos].toInt() and 0xFF) - OFFSET_SHORT_ITEM).toByte()
            return pos + 1 + length.toInt()
        }
        // []0x80]
        if (payload[pos].toInt() and 0xFF == OFFSET_SHORT_ITEM) {
            return pos + 1
        }
        // [0x00, 0x7f]
        return if (payload[pos].toInt() and 0xFF < OFFSET_SHORT_ITEM) {
            pos + 1
        } else -1
    }
}