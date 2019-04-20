package io.horizontalsystems.ethereumkit.sample

import android.app.Application
import com.facebook.stetho.Stetho

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Enable debug bridge
        Stetho.initializeWithDefaults(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }

}
