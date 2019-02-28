package io.horizontalsystems.ethereumkit.light.net.connection

import org.spongycastle.crypto.StreamCipher
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV

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