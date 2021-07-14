package com.aaronfang.fpsdetector.scene;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.aaronfang.fpsdetector.trace.RealFpsTracer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static com.aaronfang.fpsdetector.FPSConst.FACTOR_1000_POWER_3;
import static com.aaronfang.fpsdetector.FPSConst.KEY_DROP_RATE;
import static com.aaronfang.fpsdetector.FPSConst.KEY_SCENE;
import static com.aaronfang.fpsdetector.FPSConst.SCROLL_DISTANCE;
import static com.aaronfang.fpsdetector.FPSConst.SCROLL_TIME;
import static com.aaronfang.fpsdetector.FPSConst.SCROLL_VELOCITY;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector
 * with project FpsDetector
 */
public class FpsDetector {
    private static final int MIN_DROP_FRAME = 0;
    private static final Integer OFFSET_TO_MS = 100;
    private static boolean sInjectSceneChanged = true;
    private static HashSet <String> sFpsScene = new HashSet <>();
    private static RealFpsTracer mRealFpsTracer;
    private String mFromScene;
    private LinkedList<Integer> mFrameCostList;
    private Context mContext;
    private WindowManager mWindowManager;
    private View mFPSRecordView;
    private IDropFrameCallback mIDropFrameCallback;
    private IFrameCallback mIFrameCallback;
    private float mScrollSpeedX;
    private float mScrollSpeedY;
    private float mScrollDistanceX;
    private float mScrollDistanceY;
    private IFPSCallBack mIFPSCallback;
    private volatile boolean mFPSState = false;
    private long mStartTimeNanos;
    private long mLastTimeNanos;
    private int mCounter;
    private Choreographer.FrameCallback mFrameCallback;
    private FpsUtil fpsUtil;

    public static void setsFullFpsTracer(boolean sFullFpsTracer) {
        FpsDetector.sFullFpsTracer = sFullFpsTracer;
    }

    private static boolean sFullFpsTracer = false;

    @SuppressLint("ObsoleteSdkInt")
    public FpsDetector(String fromScene, Context context) {
        if (sFullFpsTracer) {
            mRealFpsTracer = new RealFpsTracer(fromScene, context);
        }
        fpsUtil = new FpsUtil(context);
        mContext = context;
        mFromScene = fromScene;
        mFrameCostList = new LinkedList <>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            mWindowManager = (WindowManager) mContext.getSystemService(Activity.WINDOW_SERVICE);
            // FPSView展示
        }
    }

    public void setFPSState(boolean fpsState) {
        this.mFPSState = mFPSState;
    }

    public interface IDropFrameCallback {
        /**
         * callback drop frames every time
         */
        void dropFrame(JSONObject dropFrames);
    }

    public void setDropFrameCallback(IDropFrameCallback dropFrameCallback) {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.setDropFrameCallback(dropFrameCallback);
        }
        this.mIDropFrameCallback = dropFrameCallback;
    }

    @SuppressWarnings("PMD")
    public interface IFPSCallBack {
        /**
         * callback fps of calculation every time
         * @param fps
         */
        void fpsCallBack(double fps);
    }

    public interface IFrameCallback{
        // time: 当前绘制帧的时间点
        void onFrame(long time);
    }

    public void setIFrameCallback(IFrameCallback callback) {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.setIFrameCallback(callback);
        }
        mIFrameCallback = callback;
    }

    public void setIFPSCallback(IFPSCallBack callback) {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.setIFPSCallback(callback);
        }
        this.mIFPSCallback = callback;
    }

    public void setScrollSpeed(float scrollSpeedX, float scrollSpeedY) {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.setScrollSpeed(scrollSpeedX, scrollSpeedY);
            return;
        }
        this.mScrollSpeedX = scrollSpeedX;
        this.mScrollSpeedY = scrollSpeedY;
    }

    public void setScrollDistance(float distanceX, float distanceY) {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.setScrollDistance(distanceX, distanceY);
            return;
        }
        this.mScrollDistanceX = distanceX;
        this.mScrollDistanceY = distanceY;
    }

    public void startRecyclerView(RecyclerView recyclerView) {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.startRecyclerView(recyclerView);
            return;
        }
        if (recyclerView == null) {
            return;
        }
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull @NotNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    start();
                }
                else {
                    stop();
                }
            }
        });
    }

    public synchronized void stop() {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.stop();
            return;
        }
        if (!mFPSState) {
            return;
        }
        calculateFPS();
        if (mFrameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(mFrameCallback);
        }
        doReport();
        mFPSState = false;
        removeScene(mFromScene);
    }

    private static void removeScene(String from) {
        sInjectSceneChanged = true;
        sFpsScene.remove(from);
    }

    private void doReport() {
        final List<Integer> reportList;
        synchronized (this) {
            if (mFrameCostList.isEmpty()) {
                return;
            }
            reportList = mFrameCostList;
            mFrameCostList = new LinkedList <>();
        }

        try {
            if (reportList.isEmpty()) {
                return;
            }
            float frameIntervalMillis = fpsUtil.getsFrameIntervalMillis();
            int refreshRate = fpsUtil.getsCurrentRefreshRate();
            int maxDropFrame = refreshRate -1;

            int[] mDropList = new int[maxDropFrame - MIN_DROP_FRAME + 1];
            int totalDuration = 0;
            for (Integer cost : reportList) {
                int droppedCount = Math.max(
                        Math.min(getDroppedCount(cost, frameIntervalMillis), maxDropFrame),
                        0
                );
                mDropList[droppedCount]++;
                totalDuration += cost / OFFSET_TO_MS;
            }

            JSONObject extraValue = new JSONObject();
            for (int i = MIN_DROP_FRAME; i <= maxDropFrame; i++) {
                if (mDropList[i] > 0) {
                    extraValue.put(String.valueOf(i), mDropList[i]);
                }
            }
            if (mIDropFrameCallback != null) {
                mIDropFrameCallback.dropFrame(JsonUtil.copyJson(extraValue));
            }

            // 维度数据
            extraValue.put(KEY_SCENE, mFromScene);

            // 掉帧时长 占 总滑动时长 之比
            extraValue.put(SCROLL_TIME, totalDuration);
            extraValue.put(SCROLL_VELOCITY, mScrollSpeedX + "," + mScrollSpeedY);
            extraValue.put(SCROLL_DISTANCE, mScrollDistanceX + "," + mScrollDistanceY);
            int sumTheoreticalFrames = (int)(totalDuration / frameIntervalMillis);
            int realFrames = reportList.size();
            extraValue.put(KEY_DROP_RATE, 1 - realFrames * 1.0f / sumTheoreticalFrames);
            Log.e("FPS-DETECTOR", extraValue.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private int getDroppedCount(Integer cost, float frameIntervalMillis) {
        int refreshRate = (int) (frameIntervalMillis * OFFSET_TO_MS);
        return ((cost + (refreshRate -1)) / refreshRate - 1);
    }

    private void calculateFPS() {
        long interval = mLastTimeNanos - mStartTimeNanos;
        if (interval <= 0 || mCounter <= 1) {
            return;
        }
        long fps = (mCounter - 1) *  FACTOR_1000_POWER_3 / interval;
        if (mIFPSCallback != null) {
            mIFPSCallback.fpsCallBack(fps);
        }
    }

    public void start() {
        if (mRealFpsTracer != null) {
            mRealFpsTracer.start();
            return;
        }
        if (mFPSState) {
            return;
        }
        resetScrollInfo();
        startFPSTracking();
        addScene(mFromScene);
        mFPSState = true;
    }

    private static void addScene(String scene) {
        sInjectSceneChanged = true;
        sFpsScene.add(scene);
    }

    private void startFPSTracking() {
        synchronized (this) {
            mFrameCostList.clear();
        }
        startFPSTrackingActual();
    }

    private void startFPSTrackingActual() {
        // reset values
        mStartTimeNanos = -1L;
        mLastTimeNanos = -1L;
        mCounter = 0;

        mFrameCallback =  new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (mStartTimeNanos == -1L) {
                    mStartTimeNanos = frameTimeNanos;
                }
                if (mIFPSCallback != null) {
                    mIFrameCallback.onFrame(frameTimeNanos / 1000000L);
                }
                mCounter++;
                if (mFPSState) {
                    Choreographer.getInstance().postFrameCallback(this);
                }
                doDropCompute(mLastTimeNanos, frameTimeNanos);
                mLastTimeNanos = frameTimeNanos;
            }
        };

        try {
            Choreographer.getInstance().postFrameCallback(mFrameCallback);
        }catch (Exception e) {
            mFPSState = false;
            mStartTimeNanos = -1L;
            mLastTimeNanos = -1L;
            mCounter = 0;
            mFrameCallback = null;
        }
    }

    private void doDropCompute(long lastTimeNanos, long frameTimeNanos) {
        if (mLastTimeNanos <= 0) {
            return;
        }

        long cost = (frameTimeNanos - lastTimeNanos) / 1000000;
        if (cost <= 0) {
            return;
        }
        synchronized (this) {
            if (mFrameCostList.size() > 20000) {
                mFrameCostList.poll();
            }
            mFrameCostList.add((int) (frameTimeNanos - lastTimeNanos) / 10000);
        }
    }

    private void resetScrollInfo() {
        mScrollDistanceX = 0;
        mScrollDistanceY = 0;
        mScrollSpeedX = 0;
        mScrollSpeedY = 0;
    }
}
