package io.horizontalsystems.ethereumkit.light.core.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import io.horizontalsystems.ethereumkit.light.models.BlockHeader
import io.horizontalsystems.ethereumkit.models.EthereumTransaction

@Database(entities = [BlockHeader::class, EthereumTransaction::class], version = 2, exportSchema = true)
abstract class SpvDatabase : RoomDatabase() {

    abstract fun blockHeaderDao(): BlockHeaderDao
    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: SpvDatabase? = null

        fun getInstance(context: Context, databaseName: String): SpvDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, databaseName).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, databaseName: String): SpvDatabase {
            return Room.databaseBuilder(context, SpvDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }
}
