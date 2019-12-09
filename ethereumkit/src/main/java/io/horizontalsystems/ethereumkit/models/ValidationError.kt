package io.horizontalsystems.ethereumkit.models

import java.lang.Exception

sealed class ValidationError: Exception() {
    object InvalidAddress: ValidationError()
    object InvalidContractAddress: ValidationError()
    object InvalidValue: ValidationError()
}