package com.videorecord;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 缓存登录信息
 */
public class PreferLogin {

    private final static String NAME = "pref_camera";
    private final static String CAMERA_INDEX = "camera_index";
    public static void putIccid(Context context, int iccid) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(CAMERA_INDEX, iccid);
        editor.apply();
    }

    public static int getIccid(Context context, int defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(CAMERA_INDEX, defaultValue);
    }



}
