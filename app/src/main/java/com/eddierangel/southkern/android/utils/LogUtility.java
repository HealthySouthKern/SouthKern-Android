package com.eddierangel.southkern.android.utils;

import android.support.constraint.BuildConfig;
import android.util.Log;

/**
 * A Log Utility that wraps Androids native Log
 * This utility will leverage the BuildConfig.DEBUG flag to protect statements
 * in the LogUtility class so they print only on debug builds of the application.
 * @see <a href="https://developer.android.com/reference/android/util/Log">Log</a>
 *
 * */
public class LogUtility {

    //Default TAG
    private static final String TAG = "LogUtility";

    /**
     * The Helper function to format strings
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    private static String getLogString(String format, Object... args) {
        //Minor optimization, only call String.format if necessary
        if (args.length == 0) {
            return format;
        }

        return String.format(format, args);
    }

    /**
     * The ERROR log levels print always
     * Uses default TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void e(String format, Object... args) {
        Log.e(TAG, getLogString(format, args));
    }

    /**
     * The WARNING log levels print always
     * Uses default TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void w(String format, Object... args) {
        Log.w(TAG, getLogString(format, args));
    }

    /**
     * The WARNING log levels print always
     * Uses default TAG
     * @throws
     * */
    public static void w(Throwable throwable) {
        Log.w(TAG, throwable);
    }

    /**
     * The INFO log levels print always
     * Uses default TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void i(String format, Object... args) {
        Log.i(TAG, getLogString(format, args));
    }

    /**
     * The ERROR log levels print always
     * @param tag TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void e(String tag,String format, Object... args) {
        Log.e(tag, getLogString(format, args));
    }

    /**
     * The WARNING log levels print always
     * @param tag TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void w(String tag,String format, Object... args) {
        Log.w(tag, getLogString(format, args));
    }

    /**
     * The WARNING log levels print always
     * @param tag TAG
     * @throws 
     * */
    public static void w(String tag, Throwable throwable) {
        Log.w(tag, throwable);
    }

    /**
     * The INFO log levels print always
     * @param tag TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void i(String tag,String format, Object... args) {
        Log.i(tag, getLogString(format, args));
    }

    /**
     * The DEBUG log levels print always
     * @param tag TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void d(String tag,String format, Object... args) {
        if (!BuildConfig.DEBUG) return;

        Log.d(tag, getLogString(format, args));
    }

    /**
     * The VERBOSE log levels print always
     * @param tag TAG
     * @param format  the string to be converted
     * @param args  the string to be converted
     * */
    public static void v(String tag,String format, Object... args) {
        if (!BuildConfig.DEBUG) return;

        Log.v(tag, getLogString(format, args));
    }
}
