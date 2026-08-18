package io.horizontalsystems.ethereumkit.utils

import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.models.Address
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.security.Security

class EIP55Test {

    companion object {
        init {
            Security.addProvider(InternalBouncyCastleProvider.getInstance())
        }
    }

    // Test vectors from the EIP-55 specification
    private val checksummedAddresses = listOf(
        "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
        "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
        "0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB",
        "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
        "0x52908400098527886E0F7030069857D2E4169EE7",
        "0x8617E340B3D01FA5F11F306F4090FD50E238070D",
        "0xde709f2102306220921060314715629080e2fb77",
        "0x27b1fdb04752bbc536007a920d24acb045561c26",
    )

    @Test
    fun format() {
        for (address in checksummedAddresses) {
            assertEquals(address, EIP55.format(address.lowercase()))
            assertEquals(address, EIP55.format(address.uppercase().replaceFirst("0X", "0x")))
        }
    }

    @Test
    fun addressEip55IsCachedAndCorrect() {
        for (address in checksummedAddresses) {
            val model = Address(address)
            assertEquals(address, model.eip55)
            assertSame(model.eip55, model.eip55)
        }
    }
}
