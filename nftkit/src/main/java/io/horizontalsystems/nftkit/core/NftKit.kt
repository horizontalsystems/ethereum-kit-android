package io.horizontalsystems.nftkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.nftkit.contracts.Eip1155ContractMethodFactories
import io.horizontalsystems.nftkit.contracts.Eip721ContractMethodFactories
import io.horizontalsystems.nftkit.core.db.NftKitDatabaseManager
import io.horizontalsystems.nftkit.models.Nft
import io.horizontalsystems.nftkit.models.NftBalance
import io.horizontalsystems.nftkit.models.NftType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigInteger

class NftKit(
    private val evmKit: EthereumKit,
    private val balanceManager: BalanceManager,
    private val balanceSyncManager: BalanceSyncManager,
    private val transactionManager: TransactionManager,
    private val storage: Storage
) : ITransactionSyncerListener {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val nftBalances: List<NftBalance>
        get() = balanceManager.nftBalances

    val nftBalancesFlow: Flow<List<NftBalance>>
        get() = balanceManager.nftBalancesFlow

    init {
        coroutineScope.launch {
            evmKit.syncStateFlowable.asFlow()
                .collect {
                    onSyncStateUpdate(it)
                }
        }
    }

    fun start() {
        coroutineScope.launch {
            balanceSyncManager.sync()
        }
    }

    fun sync() {
        if (evmKit.syncState is EthereumKit.SyncState.Synced) {
            coroutineScope.launch {
                balanceSyncManager.sync()
            }
        }
    }

    fun nftBalance(contractAddress: Address, tokenId: BigInteger): NftBalance? =
        balanceManager.nftBalance(contractAddress, tokenId)

    fun transferEip721TransactionData(contractAddress: Address, to: Address, tokenId: BigInteger): TransactionData =
        transactionManager.transferEip721TransactionData(contractAddress, to, tokenId)

    fun transferEip1155TransactionData(contractAddress: Address, to: Address, tokenId: BigInteger, value: BigInteger): TransactionData =
        transactionManager.transferEip1155TransactionData(contractAddress, to, tokenId, value)

    override fun didSync(nfts: List<Nft>, type: NftType) {
        coroutineScope.launch {
            balanceManager.didSync(nfts, type)
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun addEip1155TransactionSyncer() {
        val eip1155Syncer = Eip1155TransactionSyncer(evmKit.transactionProvider, storage)
        eip1155Syncer.listener = this

        evmKit.addTransactionSyncer(eip1155Syncer)
    }

    fun addEip721TransactionSyncer() {
        val eip721Syncer = Eip721TransactionSyncer(evmKit.transactionProvider, storage)
        eip721Syncer.listener = this

        evmKit.addTransactionSyncer(eip721Syncer)
    }

    fun addEip721Decorators() {
        evmKit.addMethodDecorator(Eip721MethodDecorator(Eip721ContractMethodFactories))
        evmKit.addEventDecorator(Eip721EventDecorator(evmKit.receiveAddress, storage))
        evmKit.addTransactionDecorator(Eip721TransactionDecorator(evmKit.receiveAddress))
    }

    fun addEip1155Decorators() {
        evmKit.addMethodDecorator(Eip1155MethodDecorator(Eip1155ContractMethodFactories))
        evmKit.addEventDecorator(Eip1155EventDecorator(evmKit.receiveAddress, storage))
        evmKit.addTransactionDecorator(Eip1155TransactionDecorator(evmKit.receiveAddress))
    }

    private suspend fun onSyncStateUpdate(syncState: EthereumKit.SyncState) {
        when (syncState) {
            is EthereumKit.SyncState.NotSynced -> Unit
            is EthereumKit.SyncState.Syncing -> Unit
            is EthereumKit.SyncState.Synced -> {
                balanceSyncManager.sync()
            }
        }
    }

    companion object {
        fun getInstance(
            context: Context,
            evmKit: EthereumKit
        ): NftKit {
            val nftKitDatabase = NftKitDatabaseManager.getNftKitDatabase(context, evmKit.chain, evmKit.walletId)
            val storage = Storage(nftKitDatabase)
            val dataProvider = DataProvider(evmKit)
            val balanceSyncManager = BalanceSyncManager(evmKit.receiveAddress, storage, dataProvider)
            val balanceManager = BalanceManager(balanceSyncManager, storage)
            val transactionManager = TransactionManager(evmKit)

            balanceSyncManager.listener = balanceManager

            return NftKit(evmKit, balanceManager, balanceSyncManager, transactionManager, storage)
        }
    }
}