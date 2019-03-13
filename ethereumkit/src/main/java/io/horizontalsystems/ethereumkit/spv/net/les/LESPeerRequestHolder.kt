package io.horizontalsystems.ethereumkit.spv.net.les

class LESPeerRequestHolder {

    private val blockHeaderRequests: MutableMap<Long, BlockHeaderRequest> = hashMapOf()
    private val accountStateRequests: MutableMap<Long, AccountStateRequest> = hashMapOf()

    fun setBlockHeaderRequest(request: BlockHeaderRequest, requestId: Long) {
        blockHeaderRequests[requestId] = request
    }

    fun removeBlockHeaderRequest(requestId: Long): BlockHeaderRequest? {
        return blockHeaderRequests.remove(requestId)
    }

    fun setAccountStateRequest(request: AccountStateRequest, requestId: Long) {
        accountStateRequests[requestId] = request
    }

    fun removeAccountStateRequest(requestId: Long): AccountStateRequest? {
        return accountStateRequests.remove(requestId)
    }
}
