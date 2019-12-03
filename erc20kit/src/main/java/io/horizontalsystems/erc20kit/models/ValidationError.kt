package io.horizontalsystems.erc20kit.models

import java.lang.Exception

sealed class ValidationError: Exception() {
    object InvalidAddress: ValidationError()
    object InvalidContractAddress: ValidationError()
    object InvalidValue: ValidationError()
}