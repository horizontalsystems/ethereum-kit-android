package io.horizontalsystems.ethereumkit.api.storage

import androidx.room.TypeConverter
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class RoomTypeConverters {
    @TypeConverter
    fun bigIntegerFromString(string: String?): BigInteger? {
        return string?.let { BigInteger(it) }
    }

    @TypeConverter
    fun bigIntegerToString(bigInteger: BigInteger?): String? {
        return bigInteger?.toString()
    }

    @TypeConverter
    fun addressFromByteArray(rawAddress: ByteArray?): Address? {
        return rawAddress?.let { Address(it) }
    }

    @TypeConverter
    fun addressToByteArray(address: Address?): ByteArray? {
        return address?.raw
    }
}
