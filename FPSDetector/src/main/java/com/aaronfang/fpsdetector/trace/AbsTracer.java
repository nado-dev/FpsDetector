package com.aaronfang.fpsdetector.trace;

import android.app.Activity;

import androidx.annotation.CallSuper;
import androidx.fragment.app.Fragment;

import com.aaronfang.fpsdetector.lifecycle.ITracer;
import com.aaronfang.fpsdetector.util.AbsLooperObserver;
import com.aaronfang.fpsdetector.util.ActivityLifeObserver;

/**
 * Created by AaFaa
 * on 2021/7/14
 * in package com.aaronfang.fpsdetector.trace
 * with project FpsDetector
 */
abstract class AbsTracer extends AbsLooperObserver implements ITracer {
    private volatile boolean isAlive = false;
    public static final String TAG = "AbsTracer";

    @CallSuper
    protected void onAlive(){

    }

    @CallSuper
    protected void onDead(){

    }

    @Override
    final synchronized public void onStartTrace() {
        if (!isAlive) {
            this.isAlive = true;
            onAlive();
        }
    }

    @Override
    public void onFront(Activity activity) {

    }

    @Override
    public void onBackground(Activity activity) {

    }

    @Override
    public void onChange(Activity activity, Fragment fragment) {

    }

    @Override
    public void onActivityCreated(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    final synchronized public void onCloseTrace() {
        if (isAlive) {
            this.isAlive = false;
            onDead();
        }
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    private boolean isForeground() {
        return ActivityLifeObserver.getInstance().isForeground();
    }


}
