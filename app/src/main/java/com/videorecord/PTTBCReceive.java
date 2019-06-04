package com.videorecord;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

/**
 * Created by fly on 2018/8/14.
 */

/**
 * 机器侧边三按键广播
 */
public class PTTBCReceive extends BroadcastReceiver {

    private static final String PTT_SHANG = "android.intent.action.button1Key"; //侧边上键
    private static final String PTT_XIA = "android.intent.action.button2Key";//侧边下键
    private static final String PTT = "android.intent.action.PTT";//ptt
    private static final String BATTERY_LOW = "android.intent.action.BATTERY_LOW";//ptt

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.e("PTTBCReceive", action);
        if (action.equals(PTT_SHANG)) {
            int keycode = intent.getIntExtra("keycode", 0);
            Log.e("PTTBCReceive", keycode + "   WWW");
            if (keycode == 2) {
//                EventBus.getDefault().post(new SwitchCameraEvent());
            } else if (keycode == 1) {

                Log.e("PTTBCReceive", "keycode  222222222222222" + MyApplication.getInstance().activitys.size());
                if (MyApplication.getInstance().activitys.size() > 0 && null != MyApplication.getInstance().getCurrentActivity()
                        && MyApplication.getInstance().getCurrentActivity().getComponentName().getClassName().equals(MainActivity.class.getName())) {
                    if (FileUtils.isBackground(context)) {
                        Log.e("PTTBCReceive", "StartVideoRecordEvent  8888888888");
                        Intent intent1 = new Intent(context, StartVideoRecordActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent1.putExtra("isStartRecord", true);
                        context.startActivity(intent1);
                        Log.e("PTTBCReceive", "StartVideoRecordEvent  44444444444");
                        return;
                    } else {
                        Log.e("PTTBCReceive", "StartVideoRecordEvent  666666666666");
                    }
                    EventBus.getDefault().post(new StartVideoRecordEvent());
                } else {
                    Log.e("PTTBCReceive", "StartVideoRecordEvent  2222222222");
                    Intent intent1 = new Intent(context, MainActivity.class);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent1.putExtra("isStartRecord", true);
                    context.startActivity(intent1);
                }
            }
        } else if (action.equals(PTT_SHANG)) {

        } else if (action.equals(PTT)) {
            if (MyApplication.getInstance().activitys.size() > 0 && null != MyApplication.getInstance().getCurrentActivity()
                    && MyApplication.getInstance().getCurrentActivity().getComponentName().getClassName().equals(MainActivity.class.getName())) {
//                if (FileUtils.isBackground(context)) {
//                    Log.e("PTTBCReceive", "StartVideoRecordEvent  8888888888");
//                    Intent intent1 = new Intent(context, StartVideoRecordActivity.class);
//                    intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    intent1.putExtra("isStartRecord", true);
//                    context.startActivity(intent1);
//                    Log.e("PTTBCReceive", "StartVideoRecordEvent  44444444444");
//                    return;
//                } else {
//                    Log.e("PTTBCReceive", "StartVideoRecordEvent  666666666666");
//                }
                int keycode = intent.getIntExtra("keycode", 0);
                Log.e("PTTBCReceive", keycode + "   ptt");
                if (keycode == 1)  {
                    EventBus.getDefault().post(new PTTDownEvent(true));
                }else if (keycode == 3){
                    EventBus.getDefault().post(new PTTDownEvent(false));
                }
            }
        }
    }

}
