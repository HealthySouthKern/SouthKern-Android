package com.eddierangel.southkern.android.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.HashMap;

// TODO: Add Documentation to Public Interface
public class PreferenceUtils {

    static Gson gson = new Gson();

    public static final String PREFERENCE_KEY_USER_ID = "userId";
    public static final String PREFERENCE_KEY_NICKNAME = "nickname";
    public static final String PREFERENCE_KEY_PROFILE_IMAGE = "profileImage";
    public static final String PREFERENCE_KEY_FIREBASE_TOKEN = "firebaseToken";
    public static final String PREFERENCE_KEY_SENDBIRD_TOKEN = "sendbirdToken";
    public static final String PREFERENCE_KEY_CONNECTED = "connected";
    public static final String PREFERENCE_KEY_FIREBASE_USER = "firebaseUser";
    public static final String PREFERENCE_KEY_FACEBOOK_TOKEN = "facebookToken";

    // Prevent instantiation
    private PreferenceUtils() {

    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("sendbird", Context.MODE_PRIVATE);
    }

    public static void setFacebookToken(Context context, String token) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_FACEBOOK_TOKEN, token).apply();
    }

    public static String getFacebookToken(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_FACEBOOK_TOKEN, "");
    }

    public static void setUser(Context context, HashMap user) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();

        // Convert hashmap to string using gson and store in preferences.
        editor.putString(PREFERENCE_KEY_FIREBASE_USER, gson.toJson(user)).apply();
    }

    public static HashMap<String, String> getUser(Context context) {
        // Convert string to hashmap using gson and return to caller.
        return gson.fromJson(getSharedPreferences(context).getString(PREFERENCE_KEY_FIREBASE_USER, ""), HashMap.class);
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

    public static void setProfileUrl(Context context, String imageUrl){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_PROFILE_IMAGE, imageUrl).apply();
    }

    public static String getProfileUrl(Context context){
        return getSharedPreferences(context).getString(PREFERENCE_KEY_PROFILE_IMAGE, "");
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
