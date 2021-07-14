package com.aaronfang.fpsdetector.lifecycle;

import android.app.Activity;

import androidx.fragment.app.Fragment;

/**
 * Created by AaFaa
 * on 2021/7/14
 * in package com.aaronfang.fpsdetector.lifecycle
 * with project FpsDetector
 */
public interface IActivityLifecycleObserver {

    void onFront(Activity activity);

    void onBackground(Activity activity);

    void onChange(Activity activity, Fragment fragment);

    void onActivityCreated(final Activity activity);

    void onActivityPaused(final Activity activity);

    void onActivityResumed(final Activity activity);

    void onActivityStarted(final Activity activity);
}
