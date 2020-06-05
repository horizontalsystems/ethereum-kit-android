package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.utils.EIP55
import java.math.BigInteger

class AddressValidator {

    open class AddressValidationException(msg: String) : Exception(msg)
    class InvalidAddressLength(msg: String) : AddressValidationException(msg)
    class InvalidAddressHex(msg: String) : AddressValidationException(msg)
    class InvalidAddressChecksum(msg: String) : AddressValidationException(msg)

    companion object {
        private const val ADDRESS_LENGTH_IN_HEX = 40

        @Throws(AddressValidationException::class)
        fun validate(address: String) {
            val cleanAddress = address.stripHexPrefix()

            check(cleanAddress.length == ADDRESS_LENGTH_IN_HEX) {
                throw InvalidAddressLength("address: $address")
            }

            try {
                BigInteger(cleanAddress, 16)
            } catch (ex: NumberFormatException) {
                throw InvalidAddressHex("address: $address")
            }

            if (isMixedCase(cleanAddress)) {
                val checksumAddress = EIP55.format(cleanAddress).stripHexPrefix()
                check(checksumAddress == cleanAddress) {
                    throw InvalidAddressChecksum("address: $address")
                }
            }
        }

        private fun isMixedCase(address: String): Boolean {
            var containsUpperCase = false
            var containsLowerCase = false

            address.forEach {
                when {
                    it.isUpperCase() -> containsUpperCase = true
                    it.isLowerCase() -> containsLowerCase = true
                }
            }
            return containsLowerCase && containsUpperCase
        }
    }

}
