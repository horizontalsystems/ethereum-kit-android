package io.horizontalsystems.erc20kit.models

sealed class TokenError: Exception() {
    class InvalidAddress: TokenError()
    class NotRegistered: TokenError()
    class AlreadyRegistered: TokenError()
}
