package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.les.messages.ProofsMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList

class BlockHeaderRequest(val blockHeader: BlockHeader, val reversed: Boolean)

class AccountStateRequest(val address: ByteArray, val blockHeader: BlockHeader) {

    fun getAccountState(proofsMessage: ProofsMessage): AccountState {
        return getValidatedState(proofsMessage, blockHeader.stateRoot, address)
    }

    private fun getValidatedState(proofsMessage: ProofsMessage, stateRoot: ByteArray, address: ByteArray): AccountState {
        val nodes = proofsMessage.nodes

        var lastNode = nodes.lastOrNull() ?: throw ProofsMessage.ProofError.NoNodes()

        check(lastNode.nodeType == TrieNode.NodeType.LEAF) {
            throw ProofsMessage.ProofError.StateNodeNotFound()
        }

        var path = lastNode.getPath(null)
                ?: throw ProofsMessage.ProofError.StateNodeNotFound()

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
                    ?: throw ProofsMessage.ProofError.NodesNotInterconnected()

            path = partialPath + path

            lastNodeKey = lastNode.hash
        }

        val addressHash = CryptoUtils.sha3(address)

        check(addressHash.toRawHexString() == path) {
            throw ProofsMessage.ProofError.PathDoesNotMatchAddressHash()
        }

        check(stateRoot.contentEquals(lastNodeKey)) {
            throw ProofsMessage.ProofError.RootHashDoesNotMatchStateRoot()
        }

        return AccountState(address, nonce, balance, storageRoot, codeHash)
    }
}
