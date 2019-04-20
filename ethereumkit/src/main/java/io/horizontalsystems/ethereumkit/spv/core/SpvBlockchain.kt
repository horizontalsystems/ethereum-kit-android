package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.net.*
import io.reactivex.Single
import java.math.BigInteger

class SpvBlockchain(private val peerGroup: PeerGroup,
                    private val storage: ISpvStorage,
                    private val transactionSigner: TransactionSigner,
                    private val transactionBuilder: TransactionBuilder,
                    override val address: ByteArray) : IBlockchain, PeerGroup.Listener {

    override var listener: IBlockchainListener? = null

    override val syncState = EthereumKit.SyncState.Synced

    override fun start() {
        peerGroup.start()
    }

    override fun stop() {
        peerGroup.stop()
    }

    override fun clear() {
    }


    override val balance: BigInteger?
        get() = storage.getAccountState()?.balance

    override fun send(rawTransaction: RawTransaction): Single<EthereumTransaction> {
        val single: Single<EthereumTransaction> = Single.create { emitter ->
            try {
                val accountState = storage.getAccountState() ?: throw NoAccountState()
                val transaction = send(rawTransaction, accountState.nonce)
                emitter.onSuccess(transaction)

            } catch (error: Exception) {
                emitter.onError(error)
            }
        }

        return single.doOnSuccess { transaction ->
            storage.saveTransactions(listOf(transaction))
            listener?.onUpdateTransactions(listOf(transaction))
        }
    }

    private fun send(rawTransaction: RawTransaction, nonce: Long): EthereumTransaction {
        val signature = transactionSigner.sign(rawTransaction, nonce)

        peerGroup.send(rawTransaction, nonce, signature)

        return transactionBuilder.transaction(rawTransaction, nonce, signature, address)
    }

    override fun onUpdate(accountState: AccountState) {
        storage.saveAccountSate(accountState)
        listener?.onUpdateBalance(accountState.balance)
    }

    override fun onUpdate(syncState: EthereumKit.SyncState) {
        listener?.onUpdateSyncState(syncState)
    }

    override val lastBlockHeight: Long?
        get() = storage.getLastBlockHeader()?.height


    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    override fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>> {
        TODO("not implemented")
    }

    override fun getStorageAt(contractAddress: ByteArray, position: ByteArray, blockNumber: Long): Single<ByteArray> {
        TODO("not implemented")
    }

    companion object {
        fun getInstance(storage: ISpvStorage, transactionSigner: TransactionSigner, transactionBuilder: TransactionBuilder, network: INetwork, address: ByteArray, nodeKey: ECKey): SpvBlockchain {
            val peerProvider = PeerProvider(nodeKey, storage, network)
            val blockValidator = BlockValidator()
            val blockHelper = BlockHelper(storage, network)
            val peerGroup = PeerGroup(storage, peerProvider, blockValidator, blockHelper, PeerGroupState(), address)
            val spvBlockchain = SpvBlockchain(peerGroup, storage, transactionSigner, transactionBuilder, address)

            peerGroup.listener = spvBlockchain

            return spvBlockchain
        }
    }

    open class SendError : Exception()
    class NoAccountState : SendError()
}
