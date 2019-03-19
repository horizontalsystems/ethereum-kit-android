package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.core.IBlockchainListener
import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.core.address
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.FeePriority
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.net.PeerGroup
import io.horizontalsystems.ethereumkit.spv.net.Ropsten
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single
import org.web3j.crypto.Keys

class SpvBlockchain(private val peerGroup: PeerGroup,
                    override val ethereumAddress: String) : IBlockchain, PeerGroup.Listener {

    override val gasPriceData: GasPrice = GasPrice.defaultGasPrice
    override val gasLimitEthereum: Int = 0
    override val gasLimitErc20: Int = 0

    override var listener: IBlockchainListener? = null

    override val blockchainSyncState = EthereumKit.SyncState.Synced

    override fun start() {
        peerGroup.start()
    }

    override fun stop() {
        peerGroup.stop()
    }

    override fun clear() {
    }

    override fun gasPriceInWei(feePriority: FeePriority): Long {
        return GasPrice.defaultGasPrice.mediumPriority
    }

    override fun syncState(contractAddress: String): EthereumKit.SyncState {
        return EthereumKit.SyncState.Synced
    }

    override fun register(contractAddress: String) {
    }

    override fun unregister(contractAddress: String) {
    }

    override fun send(toAddress: String, amount: String, feePriority: FeePriority): Single<EthereumTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: String, feePriority: FeePriority): Single<EthereumTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUpdate(state: AccountState) {
        listener?.onUpdateBalance(state.balance.toString())
    }

    companion object {

        fun spvBlockchain(storage: ISpvStorage, seed: ByteArray, testMode: Boolean): SpvBlockchain {
            val hdWallet = HDWallet(seed, if (testMode) 1 else 60)
            val formattedAddress = Keys.toChecksumAddress(hdWallet.address())

            val myKey = CryptoUtils.ecKeyFromPrivate(hdWallet.privateKey(100, 100, true).privKey)
            val peerGroup = PeerGroup(Ropsten(), storage, myKey, formattedAddress.substring(2))

            val spvBlockchain = SpvBlockchain(peerGroup, formattedAddress)

            peerGroup.listener = spvBlockchain

            return spvBlockchain
        }
    }
}
