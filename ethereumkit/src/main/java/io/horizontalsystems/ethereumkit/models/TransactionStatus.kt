package io.horizontalsystems.ethereumkit.models

enum class TransactionStatus(val value:String) {

    SUCCESS("Success"),
    FAILED("Failed"),
    PENDING("Pending"),
    NOTFOUND("Not Found")
}
