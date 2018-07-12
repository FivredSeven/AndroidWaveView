
package com.suo.waveform.clip;

import java.util.concurrent.LinkedBlockingQueue;

public class AudioQueue {

    public LinkedBlockingQueue<AudioBuffer> mQueue;

    public int mMaxSize;

    public AudioQueue() {
        mMaxSize = 500;
        ininQueue(mMaxSize);
    }

    public AudioQueue(int maxSize) {
        mMaxSize = maxSize;
        ininQueue(mMaxSize);
    }

    public void ininQueue(int size) {
        mQueue = new LinkedBlockingQueue<AudioBuffer>(size);
    }

    public int getmQueueSize() {
        return mQueue.size();
    }

    public void push(byte[] b, int size) {
        try {
            if (b != null) {
                mQueue.put(new AudioBuffer(b, size));
            } else {
                mQueue.put(new AudioBuffer(null, 0));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public AudioBuffer pop() {
        try {
            return mQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void clearCache() {
        mQueue.clear();
    }
}
