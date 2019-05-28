package io.horizontalsystems.ethereumkit.core

import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.ethereumkit.api.storage.ApiDatabase
import io.horizontalsystems.ethereumkit.core.storage.TransactionDatabase
import io.horizontalsystems.ethereumkit.spv.core.storage.SpvDatabase
import java.io.File

internal object EthereumDatabaseManager {

    fun getEthereumApiDatabase(context: Context, walletId: String, networkType: EthereumKit.NetworkType): ApiDatabase {
        val databaseName = "Ethereum-$walletId-${networkType.name}-api"
        return ApiDatabase.getInstance(context, databaseName).also { addDatabasePath(context, it) }
    }

    fun getEthereumSpvDatabase(context: Context, walletId: String, networkType: EthereumKit.NetworkType): SpvDatabase {
        val databaseName = "Ethereum-$walletId-${networkType.name}-spv"
        return SpvDatabase.getInstance(context, databaseName).also { addDatabasePath(context, it) }
    }

    fun getTransactionDatabase(context: Context, walletId: String, networkType: EthereumKit.NetworkType): TransactionDatabase {
        val databaseName = "Transactions-$walletId-${networkType.name}"
        return TransactionDatabase.getInstance(context, databaseName).also { addDatabasePath(context, it) }
    }

    fun clear(context: Context) {
        synchronized(this) {
            val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            val paths = HashSet<String>(preferences.getStringSet(DATABASE_PATHS, setOf()))

            paths.forEach { path ->
                SQLiteDatabase.deleteDatabase(File(path))
            }

            preferences.edit().clear().apply()
        }
    }

    private const val preferencesName = "ethereum_database_preferences"
    private const val DATABASE_PATHS = "key_database_paths"

    private fun addDatabasePath(context: Context, database: RoomDatabase) {
        val path = database.openHelper.writableDatabase.path

        synchronized(this) {
            val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

            val paths = HashSet<String>(preferences.getStringSet(DATABASE_PATHS, setOf()))
            paths.add(path)
            preferences.edit().putStringSet(DATABASE_PATHS, paths).apply()
        }
    }
}
