package com.videorecord;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Date;

/**
 * Created by wuwentao on 2019/3/28.
 */

public class VideoRecorderServer extends Service{

    private WindowManager windowManager;
//    private SurfaceView surfaceView;
//    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;
    private View view;
    private FrameLayout preview;

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Background Video Recorder")
                .setContentText("")
                .build();
        startForeground(1234, notification);

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        view = LayoutInflater.from(this).inflate(R.layout.background_video_record_view_layout, null);
//        surfaceView = new SurfaceView(this);
        preview= view.findViewById(R.id.sv_view);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        preview.addView(VideoRecorderManager.getInstance(this).getPreview(preview));
        windowManager.addView(preview, layoutParams);
//        surfaceView.getHolder().addCallback(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    @Override
//    public void surfaceCreated(SurfaceHolder surfaceHolder) {
//        camera = Camera.open();
//        mediaRecorder = new MediaRecorder();
//        camera.unlock();
//
//        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
//        mediaRecorder.setCamera(camera);
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//
//        mediaRecorder.setOutputFile(
//                Environment.getExternalStorageDirectory() + "/" +
//                        DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) +
//                        ".mp4"
//        );
//
//        try {
//            mediaRecorder.prepare();
//        } catch (Exception e) {
//        }
//        mediaRecorder.start();
//
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        mediaRecorder.stop();
//        mediaRecorder.reset();
//        mediaRecorder.release();
//
//        camera.lock();
//        camera.release();
//
//        windowManager.removeView(surfaceView);
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//
//    }
}
