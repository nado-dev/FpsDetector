package com.aaronfang.fpsdetector.util;

import androidx.annotation.CallSuper;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 * @author Aaron
 */
public abstract class AbsLooperObserver {
    private boolean isDispatchBegin = false;

    @CallSuper
    public void dispatchBegin(String msg) {
        isDispatchBegin = true;
    }

    @CallSuper
    public void dispatchEnd(long beginMs, long cpuBeginMs, long endMs, long cpuEndMs, boolean isBeginFrame) {
        isDispatchBegin = false;
    }

    public boolean isDispatchBegin() {
        return isDispatchBegin;
    }

    public void doFrame(String focusActivityName, long start, long end){

    }
}
