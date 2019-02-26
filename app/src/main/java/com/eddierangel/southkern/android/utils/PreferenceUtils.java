package com.eddierangel.southkern.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.api.services.calendar.model.Event;

import java.util.List;

// TODO: Add Documentation to Public Interface
public class PreferenceUtils {

    private static final String TAG = "PreferenceUtils";
    public static final String PREFERENCE_KEY_USER_ID = "userId";
    public static final String PREFERENCE_KEY_NICKNAME = "nickname";
    public static final String PREFERENCE_KEY_FIREBASE_TOKEN = "firebaseToken";
    public static final String PREFERENCE_KEY_SENDBIRD_TOKEN = "sendbirdToken";
    public static final String PREFERENCE_KEY_SOCIAL_ID = "socialId";
    public static final String PREFERENCE_KEY_SOCIAL_TYPE = "socialType";
    public static final String PREFERENCE_KEY_CONNECTED = "connected";

    // Prevent instantiation
    private PreferenceUtils() {

    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("sendbird", Context.MODE_PRIVATE);
    }

    public static void setSocialType(Context context, String type) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_SOCIAL_TYPE, type).apply();
    }

    public static String getSocialType(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_SOCIAL_TYPE, "");
    }

    public static void setSocialId(Context context, String socialId) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_SOCIAL_ID, socialId).apply();
    }

    public static String getSocialId(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_SOCIAL_ID, "");
    }

    public static void setUserId(Context context, String userId) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_USER_ID, userId).apply();
    }

    public static String getUserId(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_USER_ID, "");
    }

    public static void setNickname(Context context, String nickname) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply();
    }

    public static String getNickname(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_NICKNAME, "");
    }

    public static void setFirebaseToken(Context context, String firebaseToken) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_FIREBASE_TOKEN, firebaseToken).apply();
    }

    public static String getFirebaseToken(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_FIREBASE_TOKEN, "");
    }

    public static void setSendbirdToken(Context context, String sendbirdToken) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_SENDBIRD_TOKEN, sendbirdToken).apply();
    }

    public static String getSendbirdToken(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_SENDBIRD_TOKEN, "");
    }

    public static void setConnected(Context context, boolean tf) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(PREFERENCE_KEY_CONNECTED, tf).apply();
    }

    public static boolean getConnected(Context context) {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_CONNECTED, false);
    }

    public static void clearAll(Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.clear().apply();
    }
}
