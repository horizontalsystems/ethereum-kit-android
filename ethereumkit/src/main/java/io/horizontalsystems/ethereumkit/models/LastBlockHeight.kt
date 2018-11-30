package io.horizontalsystems.ethereumkit.models

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class LastBlockHeight : RealmObject {

    @PrimaryKey
    var id = ""

    var height: Int = 0

    constructor()

    constructor(height: Int) {
        this.height = height
    }

}
