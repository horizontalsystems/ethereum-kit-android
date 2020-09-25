package io.horizontalsystems.ethereumkit.api.jsonrpcsubscription

data class RpcBlockHeader(val number: Long)

class NewHeadsRpcSubscription : RpcSubscription<RpcBlockHeader>(listOf("newHeads")) {
    @Transient
    override val typeOfResult = RpcBlockHeader::class.java
}
