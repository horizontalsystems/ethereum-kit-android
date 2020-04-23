package io.horizontalsystems.ethereumkit.api.storage

import androidx.room.TypeConverter
import java.math.BigInteger

class RoomTypeConverters {
    @TypeConverter
    fun bigIntegerFromString(string: String): BigInteger {
        return BigInteger(string)
    }

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger): String {
        return bigInteger.toString()
    }
}
