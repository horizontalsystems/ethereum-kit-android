package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.hdwalletkit.HDWallet
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import java.math.BigInteger

fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

@Throws(NumberFormatException::class)
fun String.hexStringToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
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
