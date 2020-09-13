package io.horizontalsystems.erc20kit.core.room

import android.content.Context
import androidx.room.*
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters

@Database(entities = [Transaction::class, TokenBalance::class], version = 3, exportSchema = true)
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
        fun fromTransactionType(type: Transaction.TransactionType): String {
            return type.value
        }

        @TypeConverter
        fun toStateType(value: String?): Transaction.TransactionType? {
            return Transaction.TransactionType.valueOf(value)
        }
    }
}