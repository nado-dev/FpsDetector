package com.aaronfang.fpsdetector.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
public class ThreadWithHandler {
    private final HandlerThread mThread;
    private final Object lock = new Object();
    private volatile Handler mRealHandler;
    private final Queue<MessageEntity> mCacheQueue = new ConcurrentLinkedQueue<>();
    private final Queue<Message> mFrontCacheQueue = new ConcurrentLinkedQueue<>();

    public ThreadWithHandler(String name) {
        mThread = new InnerThread(name);
    }

    public void start() {
        mThread.start();
    }

    public boolean post(Runnable r) {
        return sendMessageDelay(getPostMessage(r), 0L);
    }

    private boolean sendMessageDelay(Message postMessage, long t) {
        if (t < 0L) {
            t = 0;
        }
        return sendMessageAtTime(postMessage, SystemClock.uptimeMillis() + t);
    }

    private Message getPostMessage(Runnable r) {
        return Message.obtain(mRealHandler, r);
    }

    private boolean sendMessageAtTime(Message msg, long upTime) {
        if (mRealHandler == null) {
            synchronized (lock) {
                if (mRealHandler == null) {
                    mCacheQueue.add(new MessageEntity(msg, upTime));
                    return true;
                }
            }
        }
        return mRealHandler.sendMessageAtTime(msg, upTime);
    }

    class InnerThread extends HandlerThread {

        public InnerThread(String name) {
            super(name);
        }

        public InnerThread(String name, int priority) {
            super(name, priority);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            synchronized (lock) {
                mRealHandler = new Handler();
            }
            mRealHandler.post(new InnerRunnable());
            while (true) {
                try{
                    Looper.loop();
                }catch (Throwable t) {

                }
            }
        }
    }

    class InnerRunnable implements Runnable{
        @Override
        public void run() {
            solveFrontCacheQueue();
            solveNormalCacheQueue();
        }

        private void solveNormalCacheQueue() {
            while (!mFrontCacheQueue.isEmpty()) {
                synchronized (lock) {
                    if (mRealHandler != null) {
                        mRealHandler.sendMessageAtFrontOfQueue(mFrontCacheQueue.poll());
                    }
                }
            }
        }

        private void solveFrontCacheQueue() {
            while (!mCacheQueue.isEmpty()) {
                synchronized (lock) {
                    MessageEntity entity = mCacheQueue.poll();
                    if (mRealHandler != null) {
                        mRealHandler.sendMessageAtTime(entity.msg, entity.time);
                    }
                }
            }
        }
    }

    static class MessageEntity{
        Message msg;
        long time;

        MessageEntity(Message message, long t) {
            this.msg = message;
            this.time = t;
        }
    }
}
