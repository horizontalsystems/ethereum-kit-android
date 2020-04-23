package io.horizontalsystems.ethereumkit.spv.core.storage

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

@Database(entities = [BlockHeader::class, AccountState::class], version = 1, exportSchema = true)
@TypeConverters(RoomTypeConverters::class)
abstract class SpvDatabase : RoomDatabase() {

    abstract fun blockHeaderDao(): BlockHeaderDao
    abstract fun accountStateDao(): AccountStateDao

    companion object {

        fun getInstance(context: Context, databaseName: String): SpvDatabase {
            return Room.databaseBuilder(context, SpvDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }
}
