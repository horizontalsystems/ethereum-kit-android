package io.horizontalsystems.ethereumkit.spv.net.handlers

import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.*
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import io.horizontalsystems.ethereumkit.spv.models.AccountStateSpv
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.les.TrieNode
import io.horizontalsystems.ethereumkit.spv.net.les.messages.GetProofsMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.ProofsMessage
import io.horizontalsystems.ethereumkit.spv.net.tasks.AccountStateTask
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList

class AccountStateTaskHandler(private var listener: Listener? = null) : ITaskHandler, IMessageHandler {

    interface Listener {
        fun didReceive(accountState: AccountStateSpv, address: Address, blockHeader: BlockHeader)
    }

    private val tasks: MutableMap<Long, AccountStateTask> = HashMap()

    private fun parse(proofsMessage: ProofsMessage, task: AccountStateTask): AccountStateSpv {
        val nodes = proofsMessage.nodes

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

        val addressHash = CryptoUtils.sha3(task.address.raw)

        check(addressHash.toRawHexString() == path) {
            throw ProofError.PathDoesNotMatchAddressHash()
        }

        check(task.blockHeader.stateRoot.contentEquals(lastNodeKey)) {
            throw ProofError.RootHashDoesNotMatchStateRoot()
        }

        return AccountStateSpv(task.address, nonce, balance, storageRoot, codeHash)
    }

    override fun perform(task: ITask, requester: ITaskHandlerRequester): Boolean {
        if (task !is AccountStateTask) {
            return false
        }

        val requestId = RandomHelper.randomLong()

        tasks[requestId] = task

        val message = GetProofsMessage(requestId, task.blockHeader.hashHex, task.address.raw)

        requester.send(message)

        return true
    }

    override fun handle(peer: IPeer, message: IInMessage): Boolean {
        if (message !is ProofsMessage) {
            return false
        }

        val task = tasks[message.requestID] ?: return false

        val accountState = parse(message, task)

        listener?.didReceive(accountState, task.address, task.blockHeader)

        return true
    }

    open class ProofError : Exception() {
        class NoNodes : ProofError()
        class StateNodeNotFound : ProofError()
        class NodesNotInterconnected : ProofError()
        class PathDoesNotMatchAddressHash : ProofError()
        class RootHashDoesNotMatchStateRoot : ProofError()
    }

}
