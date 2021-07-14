package com.aaronfang.fpsdetector.util;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Printer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
class LooperPrinterHelper {
    private static boolean isInit;
    private static PrinterListener sPrinterListener;
    private static PrinterWrapper sPrinterWrapper;
    private static Printer sOriginPrinter;

    public static void init(){
        if (isInit) {
            return;
        }
        isInit = true;
        sPrinterWrapper = new PrinterWrapper();
        sOriginPrinter = getCurrentPrinter();
        if (sOriginPrinter != null) {
            sPrinterWrapper.mPrinters.add(sOriginPrinter);
        }
        Looper.getMainLooper().setMessageLogging(sPrinterWrapper);
    }

    public static void addMessageLogging(Printer printer) {
        if (printer != null && !sPrinterWrapper.mAddedPrinters.contains(printer)) {
            sPrinterWrapper.mAddedPrinters.add(printer);
            sPrinterWrapper.haveAdded = true;
        }
    }

    private static Printer getCurrentPrinter() {
        try{
            Class <?> aClass = Class.forName("android.os.Looper");
            Field mLogging = aClass.getDeclaredField("mLogging");
            mLogging.setAccessible(true);
            return (Printer) mLogging.get(Looper.getMainLooper());
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface PrinterListener {
        void onDuration(long duration);
    }

    static class PrinterWrapper implements Printer{
        // flag: begin dispatch Msg
        public static final char START = '>';
        // flag: end dispatch Msg
        public static final char END = '<';
        private static final int MAX_COUNT = 5;
        List<Printer> mPrinters = new ArrayList <>();
        List<Printer> mRemovedPrinters = new ArrayList <>();
        List<Printer> mAddedPrinters = new ArrayList <>();
        boolean haveRemoved = false;
        boolean haveAdded = false;

        @Override
        public void println(String x) {
            if (TextUtils.isEmpty(x)) {
                return;
            }
            long start = 0;
            if (sPrinterListener != null) {
                start = System.currentTimeMillis();
            }
            if (x.charAt(0) == START) {
                if (haveAdded) {
                    for (Printer printer : mAddedPrinters){
                        if (!mPrinters.contains(printer)) {
                            mPrinters.add(printer);
                        }
                    }
                    mAddedPrinters.clear();
                    haveAdded = false;
                }
            }
            if (mPrinters.size() > MAX_COUNT) {
                Log.e("LooperPrinterHelper", "Looper wrapper contains too many printers");
            }
            for (Printer p : mPrinters) {
                if (p != null) {
                    p.println(x);
                }
            }
            if (x.charAt(0) == END) {
                if (haveRemoved) {
                    for (Printer printer: mRemovedPrinters) {
                        mPrinters.remove(printer);
                        mAddedPrinters.remove(printer);
                    }
                    mRemovedPrinters.clear();
                    haveRemoved = false;
                }
            }
            if (sPrinterListener != null && start > 0) {
                sPrinterListener.onDuration(System.currentTimeMillis() - start);
            }
        }
    }
}
