package io.horizontalsystems.ethereumkit.sample

import android.app.Application
import android.util.Log
import com.facebook.stetho.Stetho
import io.reactivex.plugins.RxJavaPlugins

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        RxJavaPlugins.setErrorHandler { e: Throwable? ->
            Log.w("RxJava ErrorHandler", e)
        }

        // Enable debug bridge
        Stetho.initializeWithDefaults(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }

}
