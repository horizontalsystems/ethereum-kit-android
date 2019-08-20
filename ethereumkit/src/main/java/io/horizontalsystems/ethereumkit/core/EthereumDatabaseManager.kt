package io.horizontalsystems.ethereumkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.api.storage.ApiDatabase
import io.horizontalsystems.ethereumkit.core.storage.TransactionDatabase
import io.horizontalsystems.ethereumkit.spv.core.storage.SpvDatabase

internal object EthereumDatabaseManager {

    fun getEthereumApiDatabase(context: Context, walletId: String, networkType: EthereumKit.NetworkType): ApiDatabase {
        return ApiDatabase.getInstance(context, getDbNameApi(walletId, networkType))
    }

    fun getEthereumSpvDatabase(context: Context, walletId: String, networkType: EthereumKit.NetworkType): SpvDatabase {
        return SpvDatabase.getInstance(context, getDbNameSpv(walletId, networkType))
    }

    fun getTransactionDatabase(context: Context, walletId: String, networkType: EthereumKit.NetworkType): TransactionDatabase {
        return TransactionDatabase.getInstance(context, getDbNameTransactions(walletId, networkType))
    }

    fun clear(context: Context, networkType: EthereumKit.NetworkType, walletId: String) {
        synchronized(this) {
            context.deleteDatabase(getDbNameApi(walletId, networkType))
            context.deleteDatabase(getDbNameSpv(walletId, networkType))
            context.deleteDatabase(getDbNameTransactions(walletId, networkType))
        }
    }

    private fun getDbNameApi(walletId: String, networkType: EthereumKit.NetworkType): String {
        return getDbName(networkType, walletId, "api")
    }

    private fun getDbNameSpv(walletId: String, networkType: EthereumKit.NetworkType): String {
        return getDbName(networkType, walletId, "spv")
    }

    private fun getDbNameTransactions(walletId: String, networkType: EthereumKit.NetworkType): String {
        return getDbName(networkType, walletId, "txs")
    }

    private fun getDbName(networkType: EthereumKit.NetworkType, walletId: String, suffix: String): String {
        return "Ethereum-${networkType.name}-$walletId-$suffix"
    }
}
