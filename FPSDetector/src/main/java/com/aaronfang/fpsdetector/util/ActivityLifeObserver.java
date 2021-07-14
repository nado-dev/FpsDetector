package com.aaronfang.fpsdetector.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aaronfang.fpsdetector.lifecycle.IActivityLifecycleManager;
import com.aaronfang.fpsdetector.lifecycle.IActivityLifecycleObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
public class ActivityLifeObserver implements Application.ActivityLifecycleCallbacks, IActivityLifecycleManager {

    private static final ActivityLifeObserver mInstance = new ActivityLifeObserver();
    private ArrayList<IActivityLifecycleObserver> mObservers = new ArrayList <>(8);
    private WeakReference<Activity> mTopActivityRef;
    private String mCurrentActivityClassName;
    private String mCurrentActivityHash;
    private boolean mIsFront;
    private boolean mChangingConfigActivity;
    private int mFrontActivityCount = 0;

    private ActivityLifeObserver(){}

    public static ActivityLifeObserver getInstance() {
        return mInstance;
    }

    public static void init(@NonNull Application application) {
        mInstance.initWithApplication(application);
    }

    private void initWithApplication(@NonNull Application application){
        if (application != null) {
            application.unregisterActivityLifecycleCallbacks(this);
            application.registerActivityLifecycleCallbacks(this);
        }
    }

    public boolean isFront(){
        return mIsFront;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        final Object[] observers = collectObservers();
        for (Object o : observers) {
            ((IActivityLifecycleObserver)o).onActivityCreated(activity);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        final Object[] observers = collectObservers();
        for (Object o : observers) {
            ((IActivityLifecycleObserver)o).onActivityStarted(activity);
        }
        // 前后台判断
        if (mChangingConfigActivity) {
            mChangingConfigActivity = false;
            return;
        }
        mFrontActivityCount++;
        if (mFrontActivityCount == 1) {
            mIsFront = true;
            notifyFront(activity);
        }
    }

    private void notifyFront(Activity activity) {
        Object[] objects = collectObservers();
        for (Object o : objects) {
            ((IActivityLifecycleObserver)o).onFront(activity);
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mTopActivityRef = new WeakReference <>(activity);
        mCurrentActivityClassName = null;
        final Object[] observers = collectObservers();
        for (Object o : observers) {
            ((IActivityLifecycleObserver)o).onActivityResumed(activity);
        }
        final String activityHash = getActivityHash(activity);
        if (!activityHash.equals(mCurrentActivityHash)) {
            for (Object o : observers) {
                ((IActivityLifecycleObserver)o).onChange(activity,null);
            }
            mCurrentActivityHash = activityHash;
        }

    }

    private String getActivityHash(Activity activity) {
        return activity.getClass().getName() + activity.hashCode();
    }

    private Object[] collectObservers() {
        Object[] callbacks = null;
        synchronized (mObservers) {
            if (mObservers.size() > 0) {
                callbacks = mObservers.toArray();
            }
        }
        if (callbacks == null) {
            return new Object[0];
        }
        return callbacks;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        final Object[] observers = collectObservers();
        for (Object o : observers) {
            ((IActivityLifecycleObserver)o).onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (activity.isChangingConfigurations()) {
            mChangingConfigActivity = true;
            return;
        }
        mFrontActivityCount--;
        if (mFrontActivityCount == 0) {
            mIsFront = false;
            notifyBackground(activity);
        }
    }

    private void notifyBackground(Activity activity) {
        final Object[] observers = collectObservers();
        for (Object o : observers) {
            ((IActivityLifecycleObserver)o).onBackground(activity);
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (getActivityHash(activity).equals(mCurrentActivityClassName)) {
            mCurrentActivityHash = null;
        }
    }

    @Override
    public boolean isForeground() {
        return false;
    }

    @Override
    public void register(IActivityLifecycleObserver observer) {
        if (mObservers != null) {
            synchronized (mObservers) {
                mObservers.add(observer);
            }
        }
    }

    @Override
    public void unregister(IActivityLifecycleObserver observer) {
        if (mObservers != null) {
            synchronized (mObservers) {
                mObservers.remove(observer);
            }
        }
    }

    public WeakReference<Activity> getTopActivityRef() {
        return mTopActivityRef;
    }

    public String getTopActivityClassName(){
        if (mTopActivityRef == null) {
            return "";
        }
        Activity topActivity = mTopActivityRef.get();
        if (topActivity != null) {
            if (mCurrentActivityClassName == null) {
                return topActivity.getClass().getCanonicalName();
            }
            else {
                return mCurrentActivityClassName;
            }
        }
        return null;
    }
}
