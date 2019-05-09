package io.horizontalsystems.erc20kit.core

import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import java.io.File

internal object Erc20DatabaseManager {

    fun getErc20Database(context: Context, contractAddress: String): Erc20KitDatabase {
        val databaseName = "Erc20-$contractAddress"
        return Erc20KitDatabase.getInstance(context, databaseName).also { addDatabasePath(context, it) }
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

    private const val preferencesName = "erc20_database_preferences"
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
