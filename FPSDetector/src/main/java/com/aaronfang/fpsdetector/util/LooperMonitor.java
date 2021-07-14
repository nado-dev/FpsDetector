package com.aaronfang.fpsdetector.util;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Printer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
public class LooperMonitor {
    private static final char CHAR_MSG_START = '>';
    private static final char CHAT_MSG_END = '<';
    private static volatile boolean sIsInited;
    public static Printer sPrinter;
    public static final CopyOnWriteArrayList<AbsLooperDispatchListener> listeners = new CopyOnWriteArrayList <AbsLooperDispatchListener>();
    public static AbsLooperDispatchListener sFirstListener;

    public static void setFirstListener(AbsLooperDispatchListener listener) {
        sFirstListener = listener;
    }

    public static void init() {
        if (sIsInited) {
            return;
        }
        sIsInited = true;
        sPrinter = new Printer() {
            @Override
            public void println(String x) {
                if (TextUtils.isEmpty(x)) {
                    return;
                }
                if (x.charAt(0) == CHAR_MSG_START) {
                    LooperMonitor.dispatch(true, x);
                }
                else if (x.charAt(0) == CHAT_MSG_END) {
                    LooperMonitor.dispatch(false,x);
                }
            }
        };
        LooperPrinterHelper.init();
        LooperPrinterHelper.addMessageLogging(sPrinter);
    }

    private static void dispatch(boolean isBegin, String msg) {
        AbsLooperDispatchListener.upTime = SystemClock.uptimeMillis();
        if (isBegin && sFirstListener != null && sFirstListener.isValid()) {
            sFirstListener.dispatchStart(msg);
        }
        for (int i = 0; i < ((List <AbsLooperDispatchListener>) listeners).size(); i++) {
            AbsLooperDispatchListener dispatchListener = listeners.get(i);
            if (dispatchListener != null && dispatchListener.isValid()) {
                if (isBegin) {
                    if (dispatchListener.isHasDispatchedStart) {
                        dispatchListener.dispatchStart(msg);
                    }
                }
                else {
                    if (dispatchListener.isHasDispatchedStart) {
                        dispatchListener.dispatchEnd(msg);
                    }
                }
            }
            else if (!isBegin && dispatchListener.isHasDispatchedStart) {
                dispatchListener.dispatchEnd("");
            }
        }
        if (!isBegin && sFirstListener != null && sFirstListener.isValid()) {
            sFirstListener.dispatchEnd("");
        }
    }
}
