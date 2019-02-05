package io.horizontalsystems.ethereumkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.math.BigDecimal

open class Balance : RealmObject {

    @PrimaryKey
    var address = ""

    var balance: String = "" // BigDecimal not supported
    var decimal: Int = 0

    constructor()

    constructor(address: String, balance: BigDecimal, decimal: Int) {
        this.address = address
        this.balance = balance.toString()
        this.decimal = decimal
    }
}
