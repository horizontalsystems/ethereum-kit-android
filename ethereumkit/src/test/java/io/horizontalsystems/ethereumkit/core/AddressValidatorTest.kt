package io.horizontalsystems.ethereumkit.core

import org.junit.Test

class AddressValidatorTest {

    private val addressValidator = AddressValidator

    @Test(expected = AddressValidator.InvalidAddressLength::class)
    fun testInvalidAddressLength() {
        addressValidator.validate("0x1234")
    }

    @Test(expected = AddressValidator.InvalidAddressHex::class)
    fun testInvalidSymbols() {
        addressValidator.validate("0x52908400098527886E0F7030069857D2E4169EEZ")
    }

    @Test(expected = AddressValidator.InvalidAddressHex::class)
    fun testInvalidPrefix() {
        addressValidator.validate("1x52908400098527886E0F7030069857D2E4169E")
    }

    @Test(expected = AddressValidator.InvalidAddressChecksum::class)
    fun testInvalidChecksum() {
        addressValidator.validate("0x52908400098527886e0F7030069857D2e4169eE7")
    }

    @Test
    fun testValidAddresses() {
        addressValidator.validate("0x52908400098527886E0F7030069857D2E4169EE7")
        addressValidator.validate("0x8617E340B3D01FA5F11F306F4090FD50E238070D")

        addressValidator.validate("0xde709f2102306220921060314715629080e2fb77")
        addressValidator.validate("0x27b1fdb04752bbc536007a920d24acb045561c26")

        addressValidator.validate("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed")
        addressValidator.validate("0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359")
        addressValidator.validate("0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB")
        addressValidator.validate("0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb")
    }

}
