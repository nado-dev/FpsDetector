package com.aaronfang.fpsdetector.scene;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector
 * with project FpsDetector
 */
public class FpsUtil {
    private Context mContext;

    public static final int REFRESH_RATE_60 = 60;
    public static final int REFRESH_RATE_90 = 90;
    public static final int REFRESH_RATE_120 = 120;
    public static final float ROUNDING = 5.1f;

    public static final int REFRESH_RATE_DEFAULT = REFRESH_RATE_60;
    public static final int MILLIS_IN_ONE_SECOND = 1000;

    private float sFrameIntervalMillis;

    public float getsFrameIntervalMillis() {
        return sFrameIntervalMillis;
    }

    public int getsCurrentRefreshRate() {
        return sCurrentRefreshRate;
    }

    public int getsDeviceMaxReFreshRate() {
        return sDeviceMaxReFreshRate;
    }

    public boolean issUsingMaxRefreshRate() {
        return sUsingMaxRefreshRate;
    }

    private int sCurrentRefreshRate;
    private int sDeviceMaxReFreshRate;
    private boolean sUsingMaxRefreshRate;

    public FpsUtil(Context context) {
        mContext = context;
        if (mContext == null) {
            sCurrentRefreshRate = sDeviceMaxReFreshRate = REFRESH_RATE_DEFAULT;
            sUsingMaxRefreshRate = true;
            sFrameIntervalMillis = MILLIS_IN_ONE_SECOND / sCurrentRefreshRate;
            return;
        }
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        float maxRate = 0;
        for (Display.Mode mode : display.getSupportedModes()) {
            float rate = mode.getRefreshRate();
            if (rate > maxRate) {
                maxRate = rate;
            }
        }
        sCurrentRefreshRate = getRoundRate(refreshRate);
        sDeviceMaxReFreshRate = getRoundRate(maxRate);
        sUsingMaxRefreshRate = sCurrentRefreshRate == sDeviceMaxReFreshRate;
        sFrameIntervalMillis = MILLIS_IN_ONE_SECOND / sCurrentRefreshRate;

        Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                refreshData(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        };
    }

    private void refreshData(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        float refreshRate = display.getRefreshRate();

        sCurrentRefreshRate = getRoundRate(refreshRate);
        sUsingMaxRefreshRate = sCurrentRefreshRate == sDeviceMaxReFreshRate;
        sFrameIntervalMillis = MILLIS_IN_ONE_SECOND / sCurrentRefreshRate;
    }

    private int getRoundRate(float refreshRate) {
        if (Math.abs(refreshRate - REFRESH_RATE_60) < ROUNDING) {
            return REFRESH_RATE_60;
        }
        else if (Math.abs(refreshRate - REFRESH_RATE_90) < ROUNDING) {
            return REFRESH_RATE_90;
        }
        else if (Math.abs(refreshRate - REFRESH_RATE_120) < ROUNDING) {
            return REFRESH_RATE_120;
        }
        return (int) refreshRate;
    }

}
