package io.horizontalsystems.ethereumkit.crypto

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
    val message: JsonElement
) {
    val sanitizedMessage: String
        get() = sanitized(message, primaryType).toString()

    private fun sanitized(json: JsonElement, type: String): JsonElement = when (json) {
        is JsonObject -> {
            val sanitizedObject = JsonObject()

            for ((key, value) in json.entrySet()) {
                val currentTypes = types[type] ?: continue
                val currentType = currentTypes.find { it.name == key } ?: continue
                sanitizedObject.add(key, sanitized(value, currentType.type))
            }
            sanitizedObject
        }

        is JsonArray -> {
            val sanitizedArray = json.map { sanitized(it, type.replace("[", "").replace("]", "")) }

            val jsonArray = JsonArray(sanitizedArray.size)
            sanitizedArray.forEach { jsonArray.add(it) }
            jsonArray
        }

        else -> {
            json
        }
    }
}

data class TypeParam(
    val name: String,
    val type: String
)
