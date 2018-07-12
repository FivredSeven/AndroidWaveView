/*
 * Copyright (c) 2014. kugou.com
 */

package com.suo.waveform.util;

import android.os.Build;

/**
 * 系统api版本判断,用来处理系统api兼用性时的判断
 * <p/>
 * <ul>
 * <li>{@link #hasDonut()} sdk 4 android 1.6</li>
 * <li>{@link #hasEclair()} sdk 5 android 2.0</li>
 * <li>{@link #hasEclair_0_1()} sdk 6 android 2.0.1</li>
 * <li>{@link #hasEclair_MR1()} sdk 7 android 2.1</li>
 * <li>{@link #hasFroyo()} sdk 8 android 2.2</li>
 * <li>{@link #hasGingerbread()} sdk 9 android 2.3</li>
 * <li>{@link #hasGingerbread_MR1()} sdk 10 android 2.3.3</li>
 * <li>{@link #hasHoneycomb()} sdk 11 android 3.0</li>
 * <li>{@link #hasHoneycombMR1()} sdk 12 android 3.1</li>
 * <li>{@link #hasHoneycombMR2()} sdk 13 android 3.2</li>
 * <li>{@link #hasIceCreamSandwich()} sdk 14 android 4.0</li>
 * <li>{@link #hasIceCreamSandwich_MR1()} sdk 15 android 4.0.3</li>
 * <li>{@link #hasJellyBean()} sdk 16 android 4.1</li>
 * <li>{@link #hasJellyBean_MR1()} sdk 17 android 4.2</li>
 * <li>{@link #hasJellyBean_MR2()} sdk 18 android 4.3</li>
 * <li>{@link #hasKitKat()} sdk 19 android 4.4</li>
 * <li>{@link #hasKitkatWatch()} sdk 20 android 4.4W</li>
 * </ul>
 */
public class ApiUtils {

    private ApiUtils() {
    }

    /**
     * sdk 4 android 1.6
     */
    public static boolean hasDonut() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT;
    }

    /**
     * sdk 5 android 2.0
     */
    public static boolean hasEclair() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;
    }

    /**
     * sdk 6 android 2.0.1
     */
    public static boolean hasEclair_0_1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_0_1;
    }

    /**
     * sdk 7 android 2.1
     */
    public static boolean hasEclair_MR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1;
    }

    /**
     * sdk 8 android 2.2
     */
    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed
        // behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * sdk 9 android 2.3
     */
    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    /**
     * sdk 10 android 2.3.3
     */
    public static boolean hasGingerbread_MR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
    }

    /**
     * sdk 11 android 3.0
     */
    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * sdk 12 android 3.1
     */
    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * sdk 13 android 3.2
     */
    public static boolean hasHoneycombMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2;
    }

    /**
     * sdk 14 android 4.0
     */
    public static boolean hasIceCreamSandwich() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * sdk 15 android 4.0.3
     */
    public static boolean hasIceCreamSandwich_MR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
    }

    /**
     * sdk 16 android 4.1
     */
    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * sdk 17 android 4.2
     */
    public static boolean hasJellyBean_MR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * sdk 18 android 4.3
     */
    public static boolean hasJellyBean_MR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * sdk 19 android 4.4
     */
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * sdk 20 android 4.4W
     */
    public static boolean hasKitkatWatch() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH;
    }

    /**
     * sdk 21 android 5.0
     */
    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasMarshmallow() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static boolean hasNougat() {
        return Build.VERSION.SDK_INT >= 24;
    }

    public static boolean hasNougatPlusPlus() {
        return Build.VERSION.SDK_INT >= 25;
    }
}

