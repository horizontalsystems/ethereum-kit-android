package io.horizontalsystems.ethereumkit.crypto

import com.google.gson.Gson
import pm.gnosis.eip712.EIP712JsonAdapter
import pm.gnosis.eip712.EIP712JsonParser
import pm.gnosis.eip712.typedDataHash
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class EIP712Encoder {

    private val eip712JsonAdapter = EIP712GsonAdapter()

    fun encodeTypedDataHash(rawJsonMessage: String): ByteArray {
        val domainWithMessage = EIP712JsonParser(eip712JsonAdapter).parseMessage(rawJsonMessage)
        return typedDataHash(domainWithMessage.message, domainWithMessage.domain)
    }

    private class EIP712GsonAdapter : EIP712JsonAdapter {
        private val gson = Gson()

        override fun parse(inputStream: InputStream): EIP712JsonAdapter.Result {
            val typedData = gson.fromJson(BufferedReader(InputStreamReader(inputStream)), TypedData::class.java)
            return parse(typedData)
        }

        override fun parse(typedDataJson: String): EIP712JsonAdapter.Result {
            val typedData = gson.fromJson(typedDataJson, TypedData::class.java)
            return parse(typedData)
        }

        private fun parse(typedData: TypedData): EIP712JsonAdapter.Result {
            return EIP712JsonAdapter.Result(
                    primaryType = typedData.primaryType,
                    domain = typedData.domain,
                    message = typedData.message,
                    types = typedData.types.mapValues { (_, types) -> types.map { EIP712JsonAdapter.Parameter(it.name, it.type) } }
            )
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
    }

}
