package io.horizontalsystems.ethereumkit.core.signer

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.core.TransactionSigner
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.EIP712Encoder
import io.horizontalsystems.ethereumkit.crypto.TypedData
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import java.math.BigInteger

class Signer(
    private val transactionBuilder: TransactionBuilder,
    private val transactionSigner: TransactionSigner,
    private val ethSigner: EthSigner
) {

    fun signature(rawTransaction: RawTransaction): Signature {
        return transactionSigner.signatureLegacy(rawTransaction)
    }

    fun signedTransaction(
        address: Address,
        value: BigInteger,
        transactionInput: ByteArray,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long
    ): ByteArray {
        val rawTransaction = RawTransaction(
            gasPrice,
            gasLimit,
            address,
            value,
            nonce,
            transactionInput
        )
        val signature = transactionSigner.signatureLegacy(rawTransaction)
        return transactionBuilder.encode(rawTransaction, signature)
    }

    fun signByteArray(message: ByteArray): ByteArray {
        return ethSigner.signByteArray(message)
    }

    fun signTypedData(rawJsonMessage: String): ByteArray {
        return ethSigner.signTypedData(rawJsonMessage)
    }

    fun parseTypedData(rawJsonMessage: String): TypedData? {
        return ethSigner.parseTypedData(rawJsonMessage)
    }

    companion object {
        fun getInstance(seed: ByteArray, networkType: EthereumKit.NetworkType): Signer {
            val privateKey = privateKey(seed, networkType)
            val address = ethereumAddress(privateKey)

            val transactionSigner = TransactionSigner(privateKey, networkType.chainId)
            val transactionBuilder = TransactionBuilder(address, networkType.chainId)
            val ethSigner = EthSigner(privateKey, CryptoUtils, EIP712Encoder())

            return Signer(transactionBuilder, transactionSigner, ethSigner)
        }

        fun address(
            words: List<String>,
            passphrase: String = "",
            networkType: EthereumKit.NetworkType
        ): Address {
            return address(Mnemonic().toSeed(words, passphrase), networkType)
        }

        fun address(seed: ByteArray, networkType: EthereumKit.NetworkType): Address {
            val privateKey = privateKey(seed, networkType)
            return ethereumAddress(privateKey)
        }

        fun privateKey(
            words: List<String>,
            passphrase: String = "",
            networkType: EthereumKit.NetworkType
        ): BigInteger {
            return privateKey(Mnemonic().toSeed(words, passphrase), networkType)
        }

        fun privateKey(seed: ByteArray, networkType: EthereumKit.NetworkType): BigInteger {
            val hdWallet = HDWallet(seed, networkType.coinType)
            return hdWallet.privateKey(0, 0, true).privKey
        }

        private fun ethereumAddress(privateKey: BigInteger): Address {
            val publicKey =
                CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1)
                    .toByteArray()
            return Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray())
        }
    }
}
