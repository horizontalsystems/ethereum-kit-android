package io.horizontalsystems.ethereumkit.core.storage

import android.content.Context
import androidx.room.*
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.models.Transaction

@Database(
        entities = [
            NotSyncTransactionRecord::class,
            Transaction::class,
            TransactionReceipt::class,
            TransactionLog::class,
            InternalTransaction::class,
            TransactionSyncerState::class,
            DroppedTransaction::class,
            TransactionTag::class,
            NotSyncedInternalTransaction::class
        ],
        version = 10,
        exportSchema = false
)
@TypeConverters(RoomTypeConverters::class, TransactionDatabase.TypeConverters::class)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun notSyncedTransactionDao(): NotSyncedTransactionDao
    abstract fun notSyncedInternalTransactionDao(): NotSyncedInternalTransactionDao
    abstract fun transactionSyncerStateDao(): TransactionSyncerStateDao

    companion object {

        fun getInstance(context: Context, databaseName: String): TransactionDatabase {
            return Room.databaseBuilder(context, TransactionDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }

    class TypeConverters {
        @TypeConverter
        fun toString(list: List<String>): String {
            return list.joinToString(separator = ",")
        }

        @TypeConverter
        fun fromString(string: String): List<String> {
            return string.split(",")
        }
    }

}
