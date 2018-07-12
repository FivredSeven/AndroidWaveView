package com.suo.waveform.util;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */

public class CommonRunnableWrapper implements Runnable {

    private static final int MAX_KEY_COUNT = 5;
    private static final Map<String, AtomicInteger> execRecord = new ConcurrentHashMap<>();
    private static volatile Field this$0;

    private final Runnable exec;
    private String key;

    CommonRunnableWrapper(Runnable exec) {
        key = genKey(exec);
        recordExecCount(key);
        this.exec = exec;
    }

    private static void recordExecCount(String key) {
        AtomicInteger value = execRecord.get(key);
        if (value == null) {
            execRecord.put(key, new AtomicInteger(1));
        } else {
            value.incrementAndGet();
        }
    }

    private static String genKey(Runnable exec) {
        try {
            if (this$0 == null) {
                synchronized (CommonRunnableWrapper.class) {
                    if (this$0 == null) {
                        this$0 = exec.getClass().getDeclaredField("this$0");
                        this$0.setAccessible(true);
                    }
                }
            }
            Object parent = this$0.get(exec);
            if (parent != null) {
                return parent.getClass().getName();
            }
        } catch (NoSuchFieldException ignore) {
        } catch (IllegalAccessException ignore) {
        } catch (Exception ignore) {
        }
        return exec.getClass().getName();
    }

    public static StringBuilder getTop5Key() {
        Map.Entry<String, AtomicInteger>[] entries = new Map.Entry[execRecord.size()];
        execRecord.entrySet().toArray(entries);
        Arrays.sort(entries, new Comparator<Map.Entry<String, AtomicInteger>>() {
            @Override
            public int compare(Map.Entry<String, AtomicInteger> lhs, Map.Entry<String, AtomicInteger> rhs) {
                return rhs.getValue().get() - lhs.getValue().get();
            }
        });

        int length = Math.min(entries.length, MAX_KEY_COUNT);
        StringBuilder sb = new StringBuilder("\n");
        for (int i = 0; i < length; i++) {
            sb.append(entries[i].getValue().get())
                    .append(" : ")
                    .append(entries[i].getKey())
                    .append("\n");
        }
        return sb;
    }

    @Override
    public void run() {
        if (exec != null) {
            exec.run();
            finalizeCount();
        }
    }

    private void finalizeCount() {
        if (!TextUtils.isEmpty(key)) {
            AtomicInteger valueRecord = execRecord.get(key);
            if (valueRecord != null) {
                valueRecord.decrementAndGet();
            }
        }
    }

}
