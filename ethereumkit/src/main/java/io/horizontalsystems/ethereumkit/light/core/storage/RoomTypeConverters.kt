package io.horizontalsystems.ethereumkit.light.core.storage

import android.arch.persistence.room.TypeConverter
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger

class RoomTypeConverters {
    @TypeConverter
    fun byteArrayFromString(string: String): ByteArray {
        return string.hexStringToByteArray()
    }

    @TypeConverter
    fun byteArrayToString(byteArray: ByteArray): String {
        return byteArray.toHexString()
    }

    @TypeConverter
    fun bigIntegerFromString(string: String): BigInteger {
        return BigInteger(string)
    }

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger): String {
        return bigInteger.toString()
    }
}
