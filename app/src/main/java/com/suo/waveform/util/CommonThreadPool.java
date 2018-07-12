package com.suo.waveform.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class CommonThreadPool {

    private static volatile CommonThreadPool sPool = null;

    private ExecutorService mPoolService = null;
//    private int CPU_COUNT=Runtime.getRuntime().availableProcessors();
//    private int THREADS=CPU_COUNT*2+5;
    private CommonThreadPool() {
//        mPoolService = Executors.newFixedThreadPool(THREADS);
        mPoolService= Executors.newCachedThreadPool();
    }

    public  static CommonThreadPool getInstance() {
        if (sPool == null) {
            init();
        }
        return sPool;
    }
    private static synchronized void init(){
        sPool = new CommonThreadPool();
    }
    public void execute(Runnable task) {
        if (task != null && !mPoolService.isShutdown()) {
            mPoolService.execute(new CommonRunnableWrapper(task));
        }
    }

    public void shutdown() {
        mPoolService.shutdown();
        sPool = null;
        mPoolService = null;
    }
}
