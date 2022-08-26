package io.horizontalsystems.nftkit.core

import io.horizontalsystems.nftkit.models.Nft
import io.horizontalsystems.nftkit.models.NftType

interface ITransactionSyncerListener {
    fun didSync(nfts: List<Nft>, type: NftType)
}

interface IBalanceSyncManagerListener {
    fun didFinishSyncBalances()
}