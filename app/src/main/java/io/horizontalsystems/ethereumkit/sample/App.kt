package io.horizontalsystems.ethereumkit.sample

import android.app.Application
import io.horizontalsystems.ethereumkit.EthereumKit

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        EthereumKit.init(this)
    }

}
