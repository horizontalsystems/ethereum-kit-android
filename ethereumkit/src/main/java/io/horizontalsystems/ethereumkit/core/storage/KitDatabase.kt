package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import io.horizontalsystems.ethereumkit.models.EthereumBalance
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.LastBlockHeight


@Database(entities = [EthereumBalance::class, GasPrice::class, LastBlockHeight::class, EthereumTransaction::class], version = 1, exportSchema = true)
abstract class KitDatabase : RoomDatabase() {

    abstract fun balanceDao(): BalanceDao
    abstract fun gasPriceDao(): GasPriceDao
    abstract fun lastBlockHeightDao(): LastBlockHeightDao
    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile private var INSTANCE: KitDatabase? = null

        fun getInstance(context: Context, databaseName: String): KitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, databaseName).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, databaseName: String): KitDatabase {
            return Room.databaseBuilder(context, KitDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }

    }

}
