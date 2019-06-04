package com.videorecord;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import java.util.ArrayList;


public class MyApplication extends Application {

    public ArrayList<Activity> activitys;
    private static MyApplication instance;
    private Activity currentActivity;
    private boolean isBackToFront;
    @Override
    public void onCreate() {
        super.onCreate();
        activitys = new ArrayList<>();
        FileUtils.getBaseCachePath();
    }

    public boolean getIsBackToFront(){
        return isBackToFront;
    }

    public void setBackToFront(boolean isBackToFront){
        this.isBackToFront = isBackToFront;
    }

    public MyApplication() {
        instance = this;
    }

    public static MyApplication getInstance() {
        return instance;
    }


    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public void setCurrentActivity(Activity currentActivity) {
        this.currentActivity = currentActivity;
    }

    // 在Activity的OnCreate方法中调用,添加Activity实例
    public void addActivity(Activity act) {
        if (activitys == null) {
            activitys = new ArrayList();
        }
        activitys.add(act);
    }

    public void removeActivity(Activity aty) {
        if (activitys.size()>0) {
            activitys.remove(aty);
        }
    }
    // 退出程序时调用，调用所有Activity的finish方法
    public void finishAll() {
        for (Activity act : activitys) {
            if (!act.isFinishing()) {
                act.finish();
            }
        }
        activitys = null;
    }
}
