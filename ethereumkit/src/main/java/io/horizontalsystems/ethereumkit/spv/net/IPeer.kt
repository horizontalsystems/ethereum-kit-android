package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature

interface IPeerListener {
    fun didConnect()
    fun didDisconnect(error: Throwable?)
    fun didReceive(blockHeaders: List<BlockHeader>, blockHeader: BlockHeader, reversed: Boolean = false)
    fun didReceive(accountState: AccountState, address: ByteArray, blockHeader: BlockHeader)
    fun didAnnounce(blockHash: ByteArray, blockHeight: Long)
}

interface IPeer {
    var listener: IPeerListener?
    val syncState: EthereumKit.SyncState

    fun connect()
    fun disconnect(error: Throwable?)
    fun requestBlockHeaders(blockHeader: BlockHeader, limit: Int, reversed: Boolean = false)
    fun requestAccountState(address: ByteArray, blockHeader: BlockHeader)

    fun send(rawTransaction: RawTransaction, signature: Signature)
}
