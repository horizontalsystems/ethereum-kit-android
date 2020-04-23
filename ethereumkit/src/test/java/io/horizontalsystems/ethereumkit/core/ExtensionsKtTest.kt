package io.horizontalsystems.ethereumkit.core

import org.junit.Test

class ExtensionsKtTest {

    @Test
    fun removeLeadingZeros() {
        val testString1 = "0000F4545"
        val testString2 = "0F4545"
        val testString3 = "F454500"

        assert(testString1.removeLeadingZeros() == "F4545")
        assert(testString2.removeLeadingZeros() == "F4545")
        assert(testString3.removeLeadingZeros() == "F454500")
    }
}