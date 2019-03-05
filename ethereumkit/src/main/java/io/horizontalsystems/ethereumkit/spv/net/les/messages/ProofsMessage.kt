package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.les.TrieNode
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList

class ProofsMessage(data: ByteArray) : IMessage {

    var requestID: Long = 0
    var bv: Long = 0
    var nodes: MutableList<TrieNode> = mutableListOf()

    init {
        val params = RLP.decode2(data)[0] as RLPList
        this.requestID = params[0].rlpData.toLong()
        this.bv = params[1].rlpData.toLong()
        val rlpList = params[2] as RLPList
        if (rlpList.isNotEmpty()) {
            for (rlpNode in rlpList) {
                nodes.add(TrieNode(rlpNode as RLPList))
            }
        }
    }

    fun getValidatedState(stateRoot: ByteArray, address: ByteArray): AccountState {

        var lastNode = nodes.lastOrNull() ?: throw ProofError.NoNodes()

        check(lastNode.nodeType == TrieNode.NodeType.LEAF) {
            throw ProofError.StateNodeNotFound()
        }

        var path = lastNode.getPath(null)
                ?: throw ProofError.StateNodeNotFound()

        val valueRLP = lastNode.elements[1]
        val value = RLP.decode2(valueRLP)[0] as RLPList

        val nonce = value[0].rlpData.toLong()
        val balance = value[1].rlpData.toBigInteger()
        val storageRoot = value[2].rlpData ?: ByteArray(0)
        val codeHash = value[3].rlpData ?: ByteArray(0)

        var lastNodeKey = lastNode.hash

        for (i in nodes.size - 2 downTo 0) {
            lastNode = nodes[i]
            val partialPath = lastNode.getPath(lastNodeKey)
                    ?: throw ProofError.NodesNotInterconnected()

            path = partialPath + path

            lastNodeKey = lastNode.hash
        }

        val addressHash = CryptoUtils.sha3(address)

        check(addressHash.toHexString() == path) {
            throw ProofError.PathDoesNotMatchAddressHash()
        }

        check(stateRoot.contentEquals(lastNodeKey)) {
            throw ProofError.RootHashDoesNotMatchStateRoot()
        }

        return AccountState(address, nonce, balance, storageRoot, codeHash)
    }

    override fun encoded(): ByteArray {
        return ByteArray(0)
    }

    override fun toString(): String {
        return "Proofs [requestID: $requestID; bv: $bv; nodes: [${nodes.joinToString(separator = ", ") { it.toString() }}]]"
    }

    open class ProofError : Exception() {
        class NoNodes : ProofError()
        class StateNodeNotFound : ProofError()
        class NodesNotInterconnected : ProofError()
        class PathDoesNotMatchAddressHash : ProofError()
        class RootHashDoesNotMatchStateRoot : ProofError()
    }
}