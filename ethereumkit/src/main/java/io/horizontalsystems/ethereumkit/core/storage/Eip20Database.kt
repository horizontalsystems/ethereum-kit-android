package io.horizontalsystems.ethereumkit.core.storage

import android.content.Context
import androidx.room.*
import io.horizontalsystems.ethereumkit.api.storage.RoomTypeConverters
import io.horizontalsystems.ethereumkit.models.Eip20Event

@Database(
    entities = [
        Eip20Event::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class, Eip20Database.TypeConverters::class)
abstract class Eip20Database : RoomDatabase() {

    abstract fun eip20EventDao(): Eip20EventDao

    companion object {

        fun getInstance(context: Context, databaseName: String): Eip20Database {
            return Room.databaseBuilder(context, Eip20Database::class.java, databaseName)
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
