package io.horizontalsystems.nftkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.core.EthereumKit
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

class NftKit(
    private val evmKit: EthereumKit,
    val balanceManager: BalanceManager,
    private val balanceSyncManager: BalanceSyncManager,
    val storage: Storage
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

    fun refresh() {
        //TODO
    }

    override fun didSync(nfts: List<Nft>, type: NftType) {
        coroutineScope.launch {
            balanceManager.didSync(nfts, type)
        }
    }

    fun stop() {
        coroutineScope.cancel()
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

            balanceSyncManager.listener = balanceManager

            return NftKit(evmKit, balanceManager, balanceSyncManager, storage)
        }

        fun addEip1155TransactionSyncer(nftKit: NftKit, evmKit: EthereumKit) {
            val eip1155Syncer = Eip1155TransactionSyncer(evmKit.transactionProvider, nftKit.storage)
            eip1155Syncer.listener = nftKit

            evmKit.addTransactionSyncer(eip1155Syncer)
        }

        fun addEip721TransactionSyncer(nftKit: NftKit, evmKit: EthereumKit) {
            val eip721Syncer = Eip721TransactionSyncer(evmKit.transactionProvider, nftKit.storage)
            eip721Syncer.listener = nftKit

            evmKit.addTransactionSyncer(eip721Syncer)
        }

        fun addEip721Decorators(nftKit: NftKit, evmKit: EthereumKit) {
            evmKit.addEventDecorator(Eip721EventDecorator(evmKit.receiveAddress, nftKit.storage))
        }

        fun addEip1155Decorators(nftKit: NftKit, evmKit: EthereumKit) {
            evmKit.addEventDecorator(Eip1155EventDecorator(evmKit.receiveAddress, nftKit.storage))
        }
    }
}