package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.core.IBlockchainListener
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.address
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.PeerGroup
import io.horizontalsystems.ethereumkit.spv.net.Ropsten
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single
import org.web3j.crypto.Keys

class SPVBlockchain(
        override val ethereumAddress: String,
        private val network: INetwork,
        private val storage: ISpvStorage) : IBlockchain, PeerGroup.Listener {

    override val gasPriceInWei: Long = 0
    override val gasLimitEthereum: Int = 0
    override val gasLimitErc20: Int = 0

    override var listener: IBlockchainListener? = null

    override val blockchainSyncState = EthereumKit.SyncState.Synced

    private val peerGroup: PeerGroup = PeerGroup(network, ethereumAddress, storage, this)

    private var started = false

    @Synchronized
    override fun start() {
        if (!started) {
            peerGroup.start()
            started = true
        }
    }

    override fun stop() {
        peerGroup.stop()
    }

    override fun clear() {
    }

    override fun syncState(contractAddress: String): EthereumKit.SyncState {
        return EthereumKit.SyncState.Synced
    }

    override fun register(contractAddress: String) {
    }

    override fun unregister(contractAddress: String) {
    }

    override fun send(toAddress: String, amount: String, gasPriceInWei: Long?): Single<EthereumTransaction> {
        TODO("not implemented")
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: String, gasPriceInWei: Long?): Single<EthereumTransaction> {
        TODO("not implemented")
    }

    override fun onUpdate(state: AccountState) {
        listener?.onUpdateBalance(state.balance.toString())
    }


    companion object {
        fun spvBlockchain(storage: ISpvStorage, seed: ByteArray, testMode: Boolean, debugPrints: Boolean = false): SPVBlockchain {

            val hdWallet = HDWallet(seed, if (testMode) 1 else 60)

            val formattedAddress = Keys.toChecksumAddress(hdWallet.address())

            return SPVBlockchain(formattedAddress, Ropsten(), storage)
        }
    }
}
