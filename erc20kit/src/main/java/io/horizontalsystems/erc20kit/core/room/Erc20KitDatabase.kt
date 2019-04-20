package io.horizontalsystems.erc20kit.core.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters

@Database(entities = [Transaction::class, TokenBalance::class], version = 1, exportSchema = true)
@TypeConverters(RoomTypeConverters::class)
abstract class Erc20KitDatabase : RoomDatabase() {

    abstract val transactionDao: TransactionDao
    abstract val tokenBalanceDao: TokenBalanceDao

    companion object {

        @Volatile
        private var INSTANCE: Erc20KitDatabase? = null

        fun getInstance(context: Context, databaseName: String): Erc20KitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, databaseName).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, databaseName: String): Erc20KitDatabase {
            return Room.databaseBuilder(context, Erc20KitDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }

}