package io.horizontalsystems.ethereumkit.network

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.horizontalsystems.ethereumkit.core.stripHexPrefix
import java.math.BigInteger

class BigIntegerTypeAdapter : TypeAdapter<BigInteger>() {
    override fun write(writer: JsonWriter, value: BigInteger) {
        writer.value(value.toString(16))
    }

    override fun read(reader: JsonReader): BigInteger {
        return BigInteger(reader.nextString().stripHexPrefix(), 16)
    }
}

class LongTypeAdapter : TypeAdapter<Long>() {
    override fun write(writer: JsonWriter, value: Long) {
        writer.value(value.toString(16))
    }

    override fun read(reader: JsonReader): Long {
        return reader.nextString().stripHexPrefix().toLong(16)
    }
}

class IntTypeAdapter : TypeAdapter<Int>() {
    override fun write(writer: JsonWriter, value: Int) {
        writer.value(value.toString(16))
    }

    override fun read(reader: JsonReader): Int {
        return reader.nextString().stripHexPrefix().toInt(16)
    }
}
