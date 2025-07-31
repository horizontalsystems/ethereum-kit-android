package io.horizontalsystems.merkleiokit

import android.content.Context
import io.horizontalsystems.ethereumkit.api.core.ApiRpcSyncer
import io.horizontalsystems.ethereumkit.api.core.NodeApiProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.core.TransactionManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import io.reactivex.Single
import java.net.URI

class MerkleTransactionAdapter(
    val blockchain: MerkleRpcBlockchain,
    val syncer: ITransactionSyncer,
    private val transactionManager: TransactionManager,
) {
    fun send(rawTransaction: RawTransaction, signature: Signature): Single<FullTransaction> {
        return blockchain.send(rawTransaction, signature)
            .map { transactionManager.handle(listOf(it)).first() }
    }

    companion object {

        private val blockchainPathMap = mapOf(
            Chain.Ethereum to "eth",
            Chain.BinanceSmartChain to "bsc",
            Chain.Base to "base"
        )

        fun getInstance(
            merkleIoPubKey: String,
            address: Address,
            chain: Chain,
            context: Context,
            walletId: String,
            transactionManager: TransactionManager,
        ): MerkleTransactionAdapter? {
            val baseUrl = "https://mempool.merkle.io/rpc/"
            val blockchainPath = blockchainPathMap[chain] ?: return null

            val url = URI("$baseUrl$blockchainPath/$merkleIoPubKey")
            val rpcProvider = NodeApiProvider(listOf(url), EthereumKit.gson)

            val connectionManager = ConnectionManager(context)
            val rpcSyncer = ApiRpcSyncer(rpcProvider, connectionManager, chain.syncInterval)

            val transactionBuilder = TransactionBuilder(address, chain.id)

            val dbName = "MerkleIo-${chain.id}-$walletId"
            val merkleDatabase = MerkleDatabase.getInstance(context, dbName)

            val merkleTransactionHashManager =
                MerkleTransactionHashManager(merkleDatabase.merkleTransactionDao())

            val blockchain = MerkleRpcBlockchain(
                address = address,
                chain = chain,
                manager = merkleTransactionHashManager,
                syncer = rpcSyncer,
                transactionBuilder = transactionBuilder
            )

            val syncer = MerkleTransactionSyncer(
                manager = merkleTransactionHashManager,
                blockchain = blockchain
            )

            return MerkleTransactionAdapter(blockchain, syncer, transactionManager)
        }
    }
}
