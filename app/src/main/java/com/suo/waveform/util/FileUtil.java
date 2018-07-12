
package com.suo.waveform.util;
import android.text.TextUtils;

/**
 * 文件操作类
 */
public class FileUtil {
    /**
     * 缓冲文件的扩展名
     */
    public static String BUFFER_EXT = ".tmp";

    /**
     * 兼容 8.3 文件名的缓冲文件的扩展名
     */
    public static String SHORT_BUFFER_EXT = ".t";

    /**
     * 加密缓冲文件的扩展名
     */
    public static String ENCRYPT_BUFFER_EXT = ".e";

    public static boolean isTempCachePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        if (path.endsWith(BUFFER_EXT) || path.endsWith(SHORT_BUFFER_EXT)
                || path.endsWith(ENCRYPT_BUFFER_EXT)) {
            return true;
        } else {
            return false;
        }
    }
}

