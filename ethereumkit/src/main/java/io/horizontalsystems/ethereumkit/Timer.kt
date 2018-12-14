package io.horizontalsystems.ethereumkit

import rx.Observable
import rx.Subscription
import java.util.concurrent.TimeUnit

class Timer(timeIntervalSeconds: Long, private val listener: Listener) {

    interface Listener {
        fun onTimeIsUp()
    }

    private var timer = Observable.interval(timeIntervalSeconds, timeIntervalSeconds, TimeUnit.SECONDS)
    private var subscription: Subscription? = null

    fun start() {
        subscription = timer.subscribe {
            listener.onTimeIsUp()
        }
    }

    fun stop() {
        subscription?.unsubscribe()
    }

}
