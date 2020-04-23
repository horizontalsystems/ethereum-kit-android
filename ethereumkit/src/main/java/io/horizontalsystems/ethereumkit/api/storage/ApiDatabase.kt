package io.horizontalsystems.ethereumkit.api.storage

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import io.horizontalsystems.ethereumkit.api.models.EthereumBalance
import io.horizontalsystems.ethereumkit.api.models.LastBlockHeight


@Database(entities = [EthereumBalance::class, LastBlockHeight::class], version = 2, exportSchema = false)
@TypeConverters(RoomTypeConverters::class)
abstract class ApiDatabase : RoomDatabase() {

    abstract fun balanceDao(): BalanceDao
    abstract fun lastBlockHeightDao(): LastBlockHeightDao

    companion object {

        fun getInstance(context: Context, databaseName: String): ApiDatabase {
            return Room.databaseBuilder(context, ApiDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }

    }

}
