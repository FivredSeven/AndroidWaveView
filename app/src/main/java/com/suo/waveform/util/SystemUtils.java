package com.suo.waveform.util;

import android.content.Context;
import android.graphics.Point;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.lang.reflect.Method;

public class SystemUtils {
    /**
     * dp转成px
     *
     * @param context
     * @param dipValue
     * @return
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 获取屏幕宽
     *
     * @param context
     * @return
     */
    @SuppressWarnings("deprecation")
    public static int getDisplayWidth(Context context) {
        if (context == null) {
            return 0;
        }
        int width = 0;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        try {
            Class<?> cls = Display.class;
            Class<?>[] parameterTypes = {Point.class};
            Point parameter = new Point();
            Method method = cls.getMethod("getSize", parameterTypes);
            method.invoke(display, parameter);
            width = parameter.x;
        } catch (Exception e) {
            width = display.getWidth();
        }
        return width;
    }

    /**
     * 获取屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 检查SD卡是否可用
     *
     * @return
     */
    public static boolean checkSDCard() {
        if (android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查SD卡的大小是否大于10M
     *
     * @return
     */
    public static boolean checkSize() {
        File path = Environment.getExternalStorageDirectory(); // 取得sdcard文件路径
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long size = (availableBlocks * blockSize) / 1024 / 1024;
        if (size < 10) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isMusicFormatSupport(String filename) {
        if (TextUtils.isEmpty(filename)) {
            return false;
        }
        int index = filename.lastIndexOf(".");
        String extension = filename.substring(index + 1, filename.length());
        if (TextUtils.isEmpty(extension)) {
            return false;
        }

        if (TextUtils.equals(extension.toLowerCase(), "mp3") ||
                TextUtils.equals(extension.toLowerCase(), "aac") ||
                TextUtils.equals(extension.toLowerCase(), "m4a") ||
                TextUtils.equals(extension.toLowerCase(), "wav")) {
            return true;
        }

        return false;
    }
}
