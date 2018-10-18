package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKitModule
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
