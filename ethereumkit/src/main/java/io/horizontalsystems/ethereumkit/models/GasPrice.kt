package io.horizontalsystems.ethereumkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class GasPrice : RealmObject {

    @PrimaryKey
    var id = ""

    var gasPriceInGwei: Double = 0.0

    constructor()

    constructor(gasPriceInGwei: Double) {
        this.gasPriceInGwei = gasPriceInGwei
    }

}
