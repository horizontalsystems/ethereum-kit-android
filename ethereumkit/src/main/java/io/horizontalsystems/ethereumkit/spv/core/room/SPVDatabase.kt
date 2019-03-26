package io.horizontalsystems.ethereumkit.spv.core.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

@Database(entities = [BlockHeader::class, EthereumTransaction::class, AccountState::class], version = 5, exportSchema = true)
abstract class SPVDatabase : RoomDatabase() {

    abstract fun blockHeaderDao(): BlockHeaderDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountStateDao(): AccountStateDao

    companion object {

        @Volatile
        private var INSTANCE: SPVDatabase? = null

        fun getInstance(context: Context, databaseName: String): SPVDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, databaseName).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, databaseName: String): SPVDatabase {
            return Room.databaseBuilder(context, SPVDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }
}
