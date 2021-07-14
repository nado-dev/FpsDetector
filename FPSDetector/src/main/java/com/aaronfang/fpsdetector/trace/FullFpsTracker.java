package com.aaronfang.fpsdetector.trace;

import android.content.Context;
import android.util.Log;

import com.aaronfang.fpsdetector.scene.FpsDetector;
import com.aaronfang.fpsdetector.scene.FpsUtil;
import com.aaronfang.fpsdetector.util.GlobalThreadPools;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.aaronfang.fpsdetector.FPSConst.KEY_DROP_RATE;
import static com.aaronfang.fpsdetector.FPSConst.KEY_SCENE;
import static com.aaronfang.fpsdetector.FPSConst.SCROLL_TIME;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.trace
 * with project FpsDetector
 */
public class FullFpsTracker extends AbsTracer {
    private ArrayList<RealFpsTracer> fpsTracers = new ArrayList <>();
    private HashMap<String, FrameCollectItem> map = new HashMap <>();
    private static Context sContext;
    private FullFpsTracker() {
        FpsDetector.setsFullFpsTracer(true);
    }

    public static void init(Context context) {
        sContext = context;
        FullFpsTracker fullFpsTracker = new FullFpsTracker();
        RealFpsTracer.setsFullFpsTracker(fullFpsTracker);
        MainThreadMonitor.Companion.getMonitor().addObserver(fullFpsTracker);
    }

    @Override
    public void doFrame(String focusActivityName, long start, long end) {
        super.doFrame(focusActivityName, start, end);
        GlobalThreadPools.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                 doDropCompute(focusActivityName, start, end);
                 for (RealFpsTracer tracer : fpsTracers) {
                     tracer.doDropCompute(start, end);
                 }
            }
        });

    }

    private void doDropCompute(String focusActivityName, long start, long end) {
        int cost = (int) (end - start);
        if (cost < 0 ){
            return;
        }

        FrameCollectItem item = map.get(focusActivityName);
        if (item == null) {
            item = new FrameCollectItem(focusActivityName);
            map.put(focusActivityName, item);
        }
        item.collect(start, end);
        if (item.sumFrameCost >= 10 * 1000) {
            map.remove(focusActivityName);
            item.report();
        }
    }

    public void addFpsTracer(final RealFpsTracer realFpsTracer) {
        GlobalThreadPools.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                fpsTracers.add(realFpsTracer);
            }
        });
    }

    public void removeFpsTracer(final RealFpsTracer realFpsTracer) {
        GlobalThreadPools.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                fpsTracers.remove(realFpsTracer);
            }
        });
    }

    private class FrameCollectItem {
        String scene;
        long sumFrameCost;
        int sumFrame = 0;
        int sumDroppedFrames;
        FpsUtil fpsUtil;

        @Override
        public String toString() {
            return "FrameCollectItem{" +
                    "scene='" + scene + '\'' +
                    ", sumFrameCost=" + sumFrameCost +
                    ", sumFrame=" + sumFrame +
                    ", sumDroppedFrames=" + sumDroppedFrames +
                    ", fpsUtil=" + fpsUtil +
                    ", dropLevel=" + Arrays.toString(dropLevel) +
                    ", maxDropCount=" + maxDropCount +
                    '}';
        }

        int[] dropLevel;
        int maxDropCount;

        FrameCollectItem(String scene) {
            this.scene = scene;
            fpsUtil = new FpsUtil(sContext);
            maxDropCount = fpsUtil.getsCurrentRefreshRate() - 1;
            dropLevel = new int[maxDropCount - 1];
        }

        void collect(long start, long end) {
            float frameIntervalMillis = fpsUtil.getsFrameIntervalMillis();
            sumFrameCost += (end - start);
            int droppedFrame = Math.max((int)((end - start) / frameIntervalMillis) , 0);
            if (droppedFrame > 42) {

            }
            droppedFrame = Math.min(droppedFrame, maxDropCount);
            dropLevel[droppedFrame]++;
            sumDroppedFrames += droppedFrame;
            sumFrame++;
        }

        void report() {
            try{
                float frameIntervalMillis = fpsUtil.getsFrameIntervalMillis();

                JSONObject extraValue = new JSONObject();
                for (int i = 0; i <= maxDropCount; i++) {
                    if (dropLevel[i] > 0) {
                        extraValue.put(String.valueOf(i), dropLevel[i]);
                    }
                }

                // 维度数据
                extraValue.put(KEY_SCENE, scene);

                // 掉帧时长 占 总滑动时长 之比
                extraValue.put(SCROLL_TIME, sumFrameCost);
                int sumTheoreticalFrames = (int)(sumFrameCost / frameIntervalMillis);
                extraValue.put(KEY_DROP_RATE, 1 - sumFrame * 1.0f / sumTheoreticalFrames);
                Log.e("FPS-DETECTOR", extraValue.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }finally {
                sumFrame = 0;
                sumDroppedFrames = 0;
                sumFrameCost = 0;
            }
        }

    }
}
