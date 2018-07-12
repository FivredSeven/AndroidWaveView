package com.suo.waveform.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

public class StorageUtils {

    private static final String SDCARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    public static final String FILE_ROOT = SDCARD_ROOT + "testDM/";

    private static final long LOW_STORAGE_THRESHOLD = 1024 * 1024 * 10;
    public static final String SV_FILTER_DIR = "sv_filter"; //短视频滤镜目录

    public static boolean isSdCardWrittenable() {

        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    /**
     * Returns individual application cache directory (for only video caching from Proxy). Cache directory will be
     * created on SD card <i>("/Android/data/[app_package_name]/cache/video-cache")</i> if card is mounted .
     * Else - Android defines cache directory on device's file system.
     *
     * @param context Application context
     * @return Cache {@link File directory}
     */
    public static File getIndividualCacheDirectory(Context context, String dir) {
        File cacheDir = getCacheDirectory(context, true);
        return new File(cacheDir, dir);
    }

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> (if card is mounted and app has appropriate permission) or
     * on device's file system depending incoming parameters.
     *
     * @param context        Application context
     * @param preferExternal Whether prefer external location for cache
     * @return Cache {@link File directory}.<br />
     * <b>NOTE:</b> Can be null in some unpredictable cases (if SD card is unmounted and
     * {@link android.content.Context#getCacheDir() Context.getCacheDir()} returns null).
     */
    private static File getCacheDirectory(Context context, boolean preferExternal) {
        File appCacheDir = null;
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) { // (sh)it happens
            externalStorageState = "";
        }
        if (preferExternal && "mounted".equals(externalStorageState)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
            String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache/";
            appCacheDir = new File(cacheDirPath);
        }
        return appCacheDir;
    }

    public static long getInnterSize() {
        File root = Environment.getRootDirectory();
        StatFs sf = new StatFs(root.getPath());
        long blockSize = sf.getBlockSize();
        long blockCount = sf.getBlockCount();
        return sf.getAvailableBlocks() * blockSize;
    }

    public static long getAvailableStorage() {

        String storageDirectory = null;
        storageDirectory = Environment.getExternalStorageDirectory().toString();

        try {
            StatFs stat = new StatFs(storageDirectory);
            long avaliableSize = ((long) stat.getAvailableBlocks() * (long) stat.getBlockSize());
            return avaliableSize;
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise).
     *
     * @param context    The context to use
     * @param uniqueName A unique directory name to append to the cache dir
     * @return The cache dir
     */
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
//        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                final String cachePath =
                        Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                                !StorageUtils.isExternalStorageRemovable() ?
                                StorageUtils.getExternalCacheDir(context).getAbsolutePath() :
                                context.getCacheDir().getAbsolutePath();

                return new File(cachePath + File.separator + uniqueName);
            } catch (Exception e) {
                e.printStackTrace();
            }
//        }
        return new File(context.getCacheDir(), uniqueName);
    }

    /***
     * 获取ExternalFilesDir路径
     *
     * @param context
     * @param uniqueName
     * @return
     */
    public static File getDiskFileDir(Context context, String uniqueName) {
        try {
            File externalFilesDir = context.getExternalFilesDir(null);
            final String filePath =
                    (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                            !StorageUtils.isExternalStorageRemovable()) && externalFilesDir != null ?
                            externalFilesDir.getAbsolutePath() :
                            context.getFilesDir().getAbsolutePath();

            return new File(filePath + File.separator + uniqueName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File(context.getFilesDir(), uniqueName);
    }

    /**
     * 音频保存路径
     * @param context
     * @return
     */
    public static File getAudioSaveFileDir(Context context) {
        return getDiskFileDir(context, "song");
    }

    /**
     * 数据库保存路径
     * @param context
     * @return
     */
    public static File getDBSaveFileDir(Context context) {
        return getDiskFileDir(context, "db");
    }

    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static long getUsableSpace(File path) {
        if (ApiUtils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     * otherwise.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static boolean isExternalStorageRemovable() {
        if (ApiUtils.hasGingerbread()) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static File getExternalCacheDir(Context context) {
        if (ApiUtils.hasFroyo()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + cacheDir);
    }

    public static boolean checkAvailableStorage() {
        if (getAvailableStorage() < LOW_STORAGE_THRESHOLD) {
            return false;
        }

        return true;
    }

    public static boolean isSDCardPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static void mkdir() throws IOException {

        File file = new File(FILE_ROOT);
        if (!file.exists() || !file.isDirectory())
            file.mkdir();
    }

    public static Bitmap getLoacalBitmap(String url) {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis); // /把流转化为Bitmap图片

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    public static String size(long size) {

        if (size / (1024 * 1024) > 0) {
            float tmpSize = (float) (size) / (float) (1024 * 1024);
            DecimalFormat df = new DecimalFormat("#.##");
            return "" + df.format(tmpSize) + "MB";
        } else if (size / 1024 > 0) {
            return "" + (size / (1024)) + "KB";
        } else
            return "" + size + "B";
    }

    public static boolean delete(File path) {

        boolean result = true;
        if (path.exists()) {
            if (path.isDirectory()) {
                for (File child : path.listFiles()) {
                    result &= delete(child);
                }
                result &= path.delete(); // Delete empty directory.
            }
            if (path.isFile()) {
                result &= path.delete();
            }
            if (!result) {
                Log.e(null, "Delete failed;");
            }
            return result;
        } else {
            Log.e(null, "File does not exist.");
            return false;
        }
    }
}
