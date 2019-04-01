package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.hdwalletkit.HDWallet
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import java.math.BigInteger

fun ByteArray?.toRawHexString(): String {
    return this?.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    } ?: ""
}

fun ByteArray?.toHexString(): String {
    val rawHex = this?.toRawHexString() ?: return ""
    return "0x$rawHex"
}

@Throws(NumberFormatException::class)
fun String.hexStringToByteArray(): ByteArray {
    val hexWithoutPrefix = this.stripHexPrefix()
    return ByteArray(hexWithoutPrefix.length / 2) {
        hexWithoutPrefix.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

fun String.stripHexPrefix(): String {
    return if (this.startsWith("0x", true)) {
        this.substring(2)
    } else {
        this
    }
}

fun HDWallet.address(): String =
        credentials().address

fun HDWallet.credentials(): Credentials {
    val accountKey = privateKey(0, 0, HDWallet.Chain.EXTERNAL.ordinal)
    val pubKey = ECKey.pubKeyFromPrivKey(accountKey.privKey, false)

    val ecKeyPair = ECKeyPair(accountKey.privKey, BigInteger(1, pubKey.slice(1 until pubKey.size).toByteArray()))

    return Credentials.create(ecKeyPair)
}
