package io.horizontalsystems.ethereumkit

import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executor


object RxBaseTest {

    fun setup() {
//        val immediate = object : Scheduler() {
//            override fun createWorker(): Scheduler.Worker {
//                return ExecutorScheduler.ExecutorWorker(Executor { it.run() })
//            }
//        }
//        //https://medium.com/@fabioCollini/testing-asynchronous-rxjava-code-using-mockito-8ad831a16877
//        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
//        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }
//        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
//        RxAndroidPlugins.setInitMainThreadSchedulerHandler{ immediate }
    }

}
