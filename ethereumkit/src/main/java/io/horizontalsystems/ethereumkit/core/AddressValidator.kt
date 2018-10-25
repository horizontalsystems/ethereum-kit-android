package io.horizontalsystems.ethereumkit.core

import org.web3j.crypto.Keys
import org.web3j.crypto.WalletUtils
import org.web3j.utils.Numeric

class AddressValidator {

    class AddressValidationException(msg: String) : Exception(msg)

    @Throws(AddressValidationException::class)
    fun validate(address: String) {

        check(WalletUtils.isValidAddress(address)) {
            throw(AddressValidationException("Invalid address format!"))
        }
        if (isMixedCase(Numeric.cleanHexPrefix(address))) {
            check(Keys.toChecksumAddress(address) == address) {
                throw(AddressValidationException("Invalid checksum!"))
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
