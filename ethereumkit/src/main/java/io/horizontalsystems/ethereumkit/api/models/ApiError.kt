package io.horizontalsystems.ethereumkit.api.models

import java.lang.Exception

sealed class ApiError : Exception() {
    object InvalidData : ApiError()
    class InfuraError(code: Int, message: String) : ApiError()
}