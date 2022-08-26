package io.horizontalsystems.nftkit.core.db

import android.content.Context
import io.horizontalsystems.ethereumkit.models.Chain

internal object NftKitDatabaseManager {

    fun getNftKitDatabase(context: Context, chain: Chain, walletId: String): NftKitDatabase {
        return NftKitDatabase.getInstance(context, getDbNameBase(chain, walletId))
    }

    fun clear(context: Context, chain: Chain, walletId: String) {
        synchronized(this) {
            val dbNameBase = getDbNameBase(chain, walletId)

            context.databaseList().forEach {
                if (it.contains(dbNameBase)) {
                    context.deleteDatabase(it)
                }
            }
        }
    }

    private fun getDbNameBase(chain: Chain, walletId: String): String {
        return "NftKit-${chain.id}-$walletId"
    }

}