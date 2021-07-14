package com.aaronfang.fpsdetector.lifecycle;

/**
 * Created by AaFaa
 * on 2021/7/14
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
public interface IActivityLifecycleManager {
    boolean isForeground();

    void register(IActivityLifecycleObserver observer);

    void unregister(IActivityLifecycleObserver observer);

}
