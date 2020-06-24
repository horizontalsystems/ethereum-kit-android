package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import java.util.*

class TrieNode(rlpList: RLPList) {

    companion object {
        private val alphabet = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    }

    enum class NodeType {
        NULL,
        BRANCH,
        EXTENSION,
        LEAF
    }

    var nodeType: NodeType = NodeType.NULL

    val elements: MutableList<ByteArray>
    var encodedPath: String? = null
    var hash: ByteArray

    init {
        this.elements = ArrayList()
        for (element in rlpList) {
            this.elements.add(element.rlpData ?: ByteArray(0))
        }

        this.hash = CryptoUtils.sha3(rlpList.rlpData ?: ByteArray(0))

        if (rlpList.size == 17) {
            this.nodeType = NodeType.BRANCH
        } else {
            val first = this.elements[0]
            val nibble = ((first[0].toInt() and 0xFF) shr 4).toByte()

            when (nibble.toInt()) {
                0 -> {
                    this.nodeType = NodeType.EXTENSION
                    encodedPath = first.copyOfRange(1, first.size).toRawHexString()
                }

                1 -> {
                    this.nodeType = NodeType.EXTENSION
                    encodedPath = first.copyOfRange(1, first.size).toRawHexString()
                    val firstByte = ((((first[0].toInt() and 0xFF) shl 4) and 0xFF) shr 4).toByte()
                    val firstByteString = byteArrayOf(firstByte).toRawHexString()
                    encodedPath = firstByteString.substring(1) + encodedPath
                }

                2 -> {
                    this.nodeType = NodeType.LEAF
                    encodedPath = first.copyOfRange(1, first.size).toRawHexString()
                }

                3 -> {
                    this.nodeType = NodeType.LEAF
                    encodedPath = first.copyOfRange(1, first.size).toRawHexString()
                    val firstByte = ((((first[0].toInt() and 0xFF) shl 4) and 0xFF) shr 4).toByte()
                    val firstByteString = byteArrayOf(firstByte).toRawHexString()
                    encodedPath = firstByteString.substring(1) + encodedPath
                }
            }
        }
    }

    fun getPath(element: ByteArray?): String? {
        if (element == null && nodeType == NodeType.LEAF) {
            return encodedPath
        }

        for (i in elements.indices) {
            if (Arrays.equals(elements[i], element)) {
                if (nodeType == NodeType.BRANCH) {
                    return alphabet[i].toString()
                } else if (nodeType == NodeType.EXTENSION) {
                    return encodedPath
                }
            }
        }
        return null
    }

    override fun toString(): String {
        return "(${elements.joinToString(separator = " | ") { it.toHexString() }})"
    }
}