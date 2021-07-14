package com.aaronfang.fpsdetector.lifecycle;

/**
 * Created by AaFaa
 * on 2021/7/14
 * in package com.aaronfang.fpsdetector.lifecycle
 * with project FpsDetector
 */
public interface ITracer extends IActivityLifecycleObserver{
    boolean isAlive();

    void onStartTrace();

    void onCloseTrace();
}
