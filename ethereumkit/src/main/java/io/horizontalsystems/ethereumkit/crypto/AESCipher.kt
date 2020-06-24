package io.horizontalsystems.ethereumkit.crypto

import org.bouncycastle.crypto.StreamCipher
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.SICBlockCipher
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV

class AESCipher(val key: ByteArray, forEncryption: Boolean ) {

    private val cipher: StreamCipher

    init {
        val encAesEngine = AESEngine()
        cipher = SICBlockCipher(encAesEngine)
        cipher.init(forEncryption, ParametersWithIV(KeyParameter(key), ByteArray(encAesEngine.blockSize)))
    }

    fun process(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        cipher.processBytes(data, 0, data.size, result, 0)
        return result
    }
}