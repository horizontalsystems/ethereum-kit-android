package io.horizontalsystems.erc20kit.core.room

import android.content.Context
import androidx.room.*
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.TransactionSyncOrder
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters

@Database(entities = [TokenBalance::class, TransactionSyncOrder::class], version = 5, exportSchema = true)
@TypeConverters(RoomTypeConverters::class)
abstract class Erc20KitDatabase : RoomDatabase() {

    abstract val tokenBalanceDao: TokenBalanceDao
    abstract val transactionSyncOrderDao: TransactionSyncOrderDao

    companion object {

        fun getInstance(context: Context, databaseName: String): Erc20KitDatabase {
            return Room.databaseBuilder(context, Erc20KitDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }

}
