package io.horizontalsystems.ethereumkit.crypto

import com.google.gson.Gson
import org.web3j.crypto.StructuredDataEncoder

class EIP712Encoder {
    private val gson = Gson()

    fun encodeTypedDataHash(rawJsonMessage: String): ByteArray {
        return StructuredDataEncoder(rawJsonMessage).hashStructuredData()
    }

    fun parseTypedData(rawJsonMessage: String): TypedData? {
        return try {
            gson.fromJson(rawJsonMessage, TypedData::class.java)
        } catch (error: Throwable) {
            null
        }
    }
}

data class TypedData(
        val types: Map<String, List<TypeParam>>,
        val primaryType: String,
        val domain: Map<String, Any>,
        val message: Map<String, Any>
)

data class TypeParam(
        val name: String,
        val type: String
)
