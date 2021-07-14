package com.aaronfang.fpsdetector.util;

import androidx.annotation.CallSuper;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
public abstract class AbsLooperDispatchListener {
    public static long upTime = 0;
    public boolean isHasDispatchedStart = false;

    public boolean isValid() {
        return false;
    }

    @CallSuper
    public void dispatchStart(String msg) {
        this.isHasDispatchedStart = true;
    }

    @CallSuper
    public void dispatchEnd(String msg) {
        this.isHasDispatchedStart = false;
    }
}
