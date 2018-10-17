package io.horizontalsystems.ethereum.kit.android

import android.app.Application
import io.horizontalsystems.ethereum.kit.EthereumKit

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        EthereumKit.init(this)
    }

}
