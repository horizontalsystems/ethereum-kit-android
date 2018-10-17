package io.horizontalsystems.ethereum.kit.core

import io.horizontalsystems.ethereum.kit.EthereumKitModule
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmFactory(databaseName: String) {

    private val configuration = RealmConfiguration.Builder()
            .name(databaseName)
            .deleteRealmIfMigrationNeeded()
            .modules(EthereumKitModule())
            .build()

    val realm: Realm
        get() = Realm.getInstance(configuration)

}
