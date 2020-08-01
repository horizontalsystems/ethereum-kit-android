package io.horizontalsystems.ethereumkit.api.storage

import androidx.room.TypeConverter
import io.horizontalsystems.ethereumkit.models.Address
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

    @TypeConverter
    fun addressFromByteArray(rawAddress: ByteArray): Address {
        return Address(rawAddress)
    }

    @TypeConverter
    fun addressToByteArray(address: Address): ByteArray {
        return address.raw
    }
}
