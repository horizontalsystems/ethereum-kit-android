package io.horizontalsystems.ethereumkit.api.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import io.horizontalsystems.ethereumkit.api.models.EthereumBalance
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight
import io.horizontalsystems.ethereumkit.models.EthereumTransaction


@Database(entities = [EthereumBalance::class, LastBlockHeight::class, EthereumTransaction::class], version = 5, exportSchema = true)
@TypeConverters(RoomTypeConverters::class)
abstract class ApiDatabase : RoomDatabase() {

    abstract fun balanceDao(): BalanceDao
    abstract fun lastBlockHeightDao(): LastBlockHeightDao
    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile private var INSTANCE: ApiDatabase? = null

        fun getInstance(context: Context, databaseName: String): ApiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, databaseName).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, databaseName: String): ApiDatabase {
            return Room.databaseBuilder(context, ApiDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }

    }

}
