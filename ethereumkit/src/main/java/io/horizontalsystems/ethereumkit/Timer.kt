package io.horizontalsystems.ethereumkit

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

class Timer(timeIntervalSeconds: Long, private val listener: Listener) {

    interface Listener {
        fun onTimeIsUp()
    }

    private var timer = Flowable.interval(timeIntervalSeconds, timeIntervalSeconds, TimeUnit.SECONDS)
    private var disposable: Disposable? = null

    fun start() {
        disposable = timer.subscribe {
            listener.onTimeIsUp()
        }
    }

    fun stop() {
        disposable?.dispose()
    }

}
