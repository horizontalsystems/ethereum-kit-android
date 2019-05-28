package io.horizontalsystems.ethereumkit.core.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters
import io.horizontalsystems.ethereumkit.models.EthereumTransaction

@Database(entities = [EthereumTransaction::class], version = 1, exportSchema = false)
@TypeConverters(RoomTypeConverters::class)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {

        fun getInstance(context: Context, databaseName: String): TransactionDatabase {
            return Room.databaseBuilder(context, TransactionDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }

}
