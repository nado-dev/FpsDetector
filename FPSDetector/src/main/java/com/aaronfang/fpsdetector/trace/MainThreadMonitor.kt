package com.aaronfang.fpsdetector.trace

import android.app.Activity
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import androidx.fragment.app.Fragment
import com.aaronfang.fpsdetector.lifecycle.IActivityLifecycleObserver
import com.aaronfang.fpsdetector.trace.MainThreadMonitor
import com.aaronfang.fpsdetector.util.*
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.trace
 * with project FpsDetector
 */
class MainThreadMonitor private constructor() : IActivityLifecycleObserver {
    private var mFrameInfo : LongArray? = null
    var isInit = false
        private set

    @Volatile
    private var isAlive = false
    var uuid: String? = null
    private val dispatchTimeMs = LongArray(4)
    private val observers: MutableList<AbsLooperObserver> = CopyOnWriteArrayList()
    private var isBeginFrame = false
    private val mFullFpsTracer = true
    private var mFrameTimeMs: Long = -1
    private val threadWithHandler = ThreadWithHandler("full")
    private var callbackRunnable: Runnable? = null
    private var callbackExist = false
    private var callbackQueueLock : Any? = null
    private var addInputQueue: Method? = null
    private var callbackQueues: Array<Any>? = null
    private val CALLBACK_INPUT = 0
    private var choreographer: Choreographer? = null

    fun init() {
        if (isInit) {
            return
        }
        if (Thread.currentThread() !== Looper.getMainLooper().thread) {
            throw AssertionError("must in main thread")
        }
        // 生命周期监控
        ActivityLifeObserver.getInstance().register(this)
        LooperMonitor.init()
        LooperMonitor.setFirstListener(object : AbsLooperDispatchListener() {
            override fun isValid(): Boolean {
                return isAlive
            }

            override fun dispatchStart(msg: String) {
                super.dispatchStart(msg)
                dispatchBegin(msg)
            }

            override fun dispatchEnd(msg: String) {
                super.dispatchEnd(msg)
                this@MainThreadMonitor.dispatchEnd()
            }
        })
        isInit = true
    }

    private fun dispatchEnd() {
        val belongFrame = isBeginFrame
        if (mFullFpsTracer) {
            if (isBeginFrame) {
                doFrameEnd()
                val end = AbsLooperDispatchListener.upTime
                val begin = mFrameTimeMs
                threadWithHandler.post {
                    for (observer in observers) {
                        observer.doFrame(
                            ActivityLifeObserver.getInstance().topActivityClassName,
                            begin,
                            end
                        )
                    }
                }
            }
        }
        dispatchTimeMs[1] = AbsLooperDispatchListener.upTime
        dispatchTimeMs[3] = SystemClock.currentThreadTimeMillis()
        val list: List<AbsLooperObserver> = observers
        for (observer in list) {
            if (observer.isDispatchBegin) {
                observer.dispatchEnd(
                    dispatchTimeMs[0],
                    dispatchTimeMs[1],
                    dispatchTimeMs[2],
                    dispatchTimeMs[3],
                    belongFrame
                )
            }
        }
    }

    private fun doFrameEnd() {
        addFrameCallback(callbackRunnable)
        isBeginFrame = false
    }

    @Synchronized
    private fun addFrameCallback(callbackRunnable: Runnable?) {
        if (!isAlive) {
            return
        }
        if (callbackExist) {
            return
        }
        try {
            callbackQueueLock?.let {
                synchronized(it) {
                    val method = addInputQueue
                    if (method != null) {
                        method.invoke(
                            callbackQueues?.get(CALLBACK_INPUT),
                            (-1).toLong(),
                            callbackRunnable,
                            null
                        )
                        callbackExist = true
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun dispatchBegin(msg: String) {
        uuid = null
        dispatchTimeMs[0] = AbsLooperDispatchListener.upTime
        dispatchTimeMs[2] = SystemClock.currentThreadTimeMillis()
        val listeners: List<AbsLooperObserver> = observers
        for (looperObserver in listeners) {
            if (looperObserver.isDispatchBegin) {
                looperObserver.dispatchBegin(msg)
            }
        }
    }

    fun addObserver(observer: AbsLooperObserver) {
        if (!isAlive) {
            onStart()
        }
        if (!observers.contains(observer)) {
            observers.add(observer)
        }
    }

    fun removeObserver(observer: AbsLooperObserver) {
        observers.remove(observer)
        if (observers.isEmpty()) {
            onStop()
        }
    }

    private fun doFrameBegin() {
        isBeginFrame = true
    }

    private fun doInputCallbackHook() {
        try {
            if (mFrameInfo == null) {
                mFrameTimeMs = AbsLooperDispatchListener.upTime
            }
            else {
                mFrameTimeMs = mFrameInfo?.get(1)?.div(1000000) ?: AbsLooperDispatchListener.upTime
            }
            doFrameBegin()

        }catch (e : Exception) {

        }finally {
            callbackExist = false
        }
    }

    private fun onStop() {
        if (!isInit) {
            return
        }
        if (isAlive) {
            isAlive = false
        }
    }

    @Synchronized
    private fun onStart() {
        if (!isInit) {
            return
        }
        if (!isAlive) {
            isAlive = true
        }
        addFrameCallback(callbackRunnable)
    }

    override fun onFront(activity: Activity) {
        if (choreographer != null) {
            return
        }
        try {
            choreographer = Choreographer.getInstance()
        } catch (ignore: Exception) {
        }
        thread {
            try {
                callbackRunnable = Runnable {
                    try {
                        doInputCallbackHook();
                    }catch (e : Throwable) {}
                }

                callbackQueueLock = ReflectUtil.reflectObject(choreographer, "mLock")
                if (callbackQueueLock == null) {
                    callbackQueueLock = ReflectUtil.reflectHideObject(choreographer, "mLock")
                }
                callbackQueues = ReflectUtil.reflectObject(choreographer, "mCallbackQueues")
                if (callbackQueues == null) {
                    callbackQueues = ReflectUtil.reflectHideObject(choreographer, "mCallbackQueues")
                }
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                    mFrameInfo = ReflectUtil.reflectHideObject( ReflectUtil.reflectHideObject(choreographer, "mFrameInfo"), "mFrameInfo")
                }
                else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    mFrameInfo = ReflectUtil.reflectHideObject( ReflectUtil.reflectHideObject(choreographer, "mFrameInfo"), "frameInfo")
                }
                else
                    mFrameInfo = ReflectUtil.reflectObject( ReflectUtil.reflectObject(choreographer, "mFrameInfo"), "mFrameInfo")

                addInputQueue = ReflectUtil.reflectChoreographerMethod(callbackQueues?.get(0), "addCallbackLocked")
                addFrameCallback(callbackRunnable)
            }catch (e : Exception) {

            }
        }
    }

    override fun onBackground(activity: Activity) {}
    override fun onChange(activity: Activity, fragment: Fragment) {}
    override fun onActivityCreated(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityStarted(activity: Activity) {}

    companion object {
        val monitor = MainThreadMonitor()
    }

    init {
        threadWithHandler.start()
    }
}