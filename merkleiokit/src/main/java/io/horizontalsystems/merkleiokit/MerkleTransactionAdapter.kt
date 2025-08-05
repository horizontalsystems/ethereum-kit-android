package io.horizontalsystems.merkleiokit

import android.content.Context
import io.horizontalsystems.ethereumkit.api.core.ApiRpcSyncer
import io.horizontalsystems.ethereumkit.api.core.NodeApiProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
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
    val syncer: MerkleTransactionSyncer,
    private val transactionManager: TransactionManager,
    private val sourceTag: String,
) {
    fun send(rawTransaction: RawTransaction, signature: Signature): Single<FullTransaction> {
        return blockchain.send(rawTransaction, signature, sourceTag)
            .map { transactionManager.handle(listOf(it)).first() }
    }

    fun registerInKit(ethereumKit: EthereumKit) {
        ethereumKit.addNonceProvider(blockchain)
        ethereumKit.addTransactionSyncer(syncer)
        ethereumKit.addExtraDecorator(syncer)
    }

    companion object {
        val protectedKey = "protected"

        private val blockchainPathMap = mapOf(
            Chain.Ethereum to "eth",
            Chain.BinanceSmartChain to "bsc",
            Chain.Base to "base"
        )

        fun isProtected(transaction: FullTransaction): Boolean {
            return transaction.extra[protectedKey] == true
        }

        fun getInstance(
            merkleIoPubKey: String,
            address: Address,
            chain: Chain,
            context: Context,
            walletId: String,
            transactionManager: TransactionManager,
            sourceTag: String,
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
                manager = merkleTransactionHashManager,
                syncer = rpcSyncer,
                transactionBuilder = transactionBuilder
            )

            val syncer = MerkleTransactionSyncer(
                manager = merkleTransactionHashManager,
                blockchain = blockchain,
                transactionManager = transactionManager
            )

            return MerkleTransactionAdapter(blockchain, syncer, transactionManager, sourceTag)
        }
    }
}
