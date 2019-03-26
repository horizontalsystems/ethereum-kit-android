package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.net.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single
import org.web3j.crypto.Keys

class SpvBlockchain(private val peerGroup: PeerGroup,
                    private val storage: ISpvStorage,
                    private val network: INetwork,
                    private val transactionSigner: TransactionSigner,
                    override val address: String) : IBlockchain, PeerGroup.Listener {

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

    override fun syncStateErc20(contractAddress: String): EthereumKit.SyncState {
        return EthereumKit.SyncState.Synced
    }

    override fun register(contractAddress: String) {
    }

    override fun unregister(contractAddress: String) {
    }

    override val balance: String?
        get() = storage.getAccountState()?.balance?.toString()

    private fun sendInternal(toAddress: String, amount: String, gasPrice: Long, gasLimit: Long): EthereumTransaction {
        val accountState = storage.getAccountState() ?: throw NoAccountState()
        val nonce = accountState.nonce

        val rawTransaction = RawTransaction(nonce.toBigInteger(), gasPrice.toBigInteger(), gasLimit.toBigInteger(), toAddress, amount.toBigInteger())
        val signature = transactionSigner.sign(rawTransaction)

        peerGroup.send(rawTransaction, signature)

        val txHash = transactionSigner.hash(rawTransaction, signature)
        val transaction = EthereumTransaction().apply {
            this.hash = txHash.toHexString()
            this.nonce = nonce
            this.from = address
            this.to = toAddress
            this.value = amount
            this.gasLimit = gasLimit
            this.gasPriceInWei = gasPrice
        }

        storage.saveTransactions(listOf(transaction))

        return transaction
    }

    override fun send(toAddress: String, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction> {
        return Single.create {
            try {
                val transaction = sendInternal(toAddress, amount, gasPrice, gasLimit)
                it.onSuccess(transaction)
            } catch (ex: Exception) {
                ex.printStackTrace()
                it.onError(ex)
            }
        }
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onUpdate(accountState: AccountState) {
        storage.saveAccountSate(accountState)
        listener?.onUpdateBalance(accountState.balance.toString())
    }

    override fun onUpdate(syncState: EthereumKit.SyncState) {
        listener?.onUpdateState(syncState)
    }

    override fun getLastBlockHeight(): Long? {
        return storage.getLastBlockHeader()?.height
    }

    override fun getBalanceErc20(contractAddress: String): String? {
        return null
    }

    override fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, contractAddress)
    }

    companion object {

        fun spvBlockchain(storage: ISpvStorage, seed: ByteArray, testMode: Boolean): SpvBlockchain {
            val hdWallet = HDWallet(seed, if (testMode) 1 else 60)
            val formattedAddress = Keys.toChecksumAddress(hdWallet.address())

            val network = Ropsten()
            val myKey = CryptoUtils.ecKeyFromPrivate(hdWallet.privateKey(102, 102, true).privKey)
            val peerProvider = PeerProvider(myKey, storage, network)
            val blockValidator = BlockValidator()
            val blockHelper = BlockHelper(storage, network)
            val address = formattedAddress.substring(2).hexStringToByteArray()
            val peerGroup = PeerGroup(storage, peerProvider, blockValidator, blockHelper, PeerGroupState(), address)
            val privateKey = hdWallet.privateKey(0, 0, true).privKey
            val transactionSigner = TransactionSigner(network.id, privateKey)
            val spvBlockchain = SpvBlockchain(peerGroup, storage, network, transactionSigner, formattedAddress)

            peerGroup.listener = spvBlockchain

            return spvBlockchain
        }
    }

    open class SendError : Exception()
    class NoAccountState : SendError()
}
