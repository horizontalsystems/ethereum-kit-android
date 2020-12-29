package io.horizontalsystems.erc20kit.core.room

import android.content.Context
import androidx.room.*
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.TransactionRecord
import io.horizontalsystems.erc20kit.models.TransactionType
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters

@Database(entities = [TransactionRecord::class, TokenBalance::class], version = 4, exportSchema = true)
@TypeConverters(RoomTypeConverters::class, Erc20KitDatabase.TypeConverters::class)
abstract class Erc20KitDatabase : RoomDatabase() {

    abstract val transactionDao: TransactionDao
    abstract val tokenBalanceDao: TokenBalanceDao

    companion object {

        fun getInstance(context: Context, databaseName: String): Erc20KitDatabase {
            return Room.databaseBuilder(context, Erc20KitDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }

    class TypeConverters {
        @TypeConverter
        fun fromTransactionType(type: TransactionType): String {
            return type.value
        }

        @TypeConverter
        fun toStateType(value: String?): TransactionType? {
            return TransactionType.valueOf(value)
        }
    }
}