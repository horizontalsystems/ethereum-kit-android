package io.horizontalsystems.merkleiokit

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MerkleTransactionHash::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class MerkleDatabase : RoomDatabase() {
    abstract fun merkleTransactionDao(): MerkleTransactionDao

    companion object Companion {
        fun getInstance(context: Context, databaseName: String): MerkleDatabase {
            return Room.databaseBuilder(context, MerkleDatabase::class.java, databaseName)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
        }
    }
}
