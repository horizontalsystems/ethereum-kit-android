package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain

internal object Erc20DatabaseManager {

    fun getErc20Database(context: Context, chain: Chain, walletId: String, contractAddress: Address): Erc20KitDatabase {
        return Erc20KitDatabase.getInstance(context, "${getDbNameBase(chain, walletId)}-${contractAddress.hex}")
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
        return "Erc20-${chain.id}-$walletId"
    }

}
