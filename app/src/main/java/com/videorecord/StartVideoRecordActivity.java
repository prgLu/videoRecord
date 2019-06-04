package com.videorecord;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by wuwentao on 2019/3/30.
 */

public class StartVideoRecordActivity extends BaseActivity implements View.OnClickListener {
    private static String TAG = "StartVideoRecordActivity";
    private FrameLayout preview;
    private ImageView ivRecordingSwitch, ivFlash, ivCameraSwitch, video_list;
    private TextView tvComplete, timer;
    static boolean isRecording;
    int currentCameraFacing = 0;//0后置，1前置
    VideoRecorderManager videoRecorderManager;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        preview = (FrameLayout) findViewById(R.id.sv_view);
        ivRecordingSwitch = (ImageView) findViewById(R.id.iv_movieRecorder);
        ivFlash = (ImageView) findViewById(R.id.iv_flash);
        ivCameraSwitch = (ImageView) findViewById(R.id.iv_switch_camera);
        tvComplete = (TextView) findViewById(R.id.tv_complete_movieRecorger);
        video_list = (ImageView) findViewById(R.id.video_list);
        timer = (TextView) findViewById(R.id.timer);
        ivRecordingSwitch.setOnClickListener(this);
        ivFlash.setOnClickListener(this);
        ivCameraSwitch.setOnClickListener(this);
        video_list.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //申请权限，REQUEST_TAKE_PHOTO_PERMISSION是自定义的常量
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    0);
        } else if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0);
        } else if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        } else if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        } else {
            Log.e(TAG, "PERMISSION_GRANTED   " + Camera.getNumberOfCameras());
            currentCameraFacing = PreferLogin.getIccid(this, 1);
            videoRecorderManager = VideoRecorderManager.getInstance(this);
            videoRecorderManager.setCameraInstance(currentCameraFacing);
            preview.addView(videoRecorderManager.getPreview(preview));
        }
        if (getIntent().getBooleanExtra("isStartRecord", false)) {
            handler.sendEmptyMessageDelayed(1000, 500);
        }
        deleteVideoThread();
    }

    private void deleteVideoThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtils.deleteOldVideo();
            }
        }).start();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1000) {
                if (videoRecorderManager.prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    videoRecorderManager.getMediaRecorder().start();
                    handler.post(mTimestampRunnable);
                    isRecording = true;
                }
            } else if (msg.what == 2000) {

            }
        }
    };
    private long mTalkTimeSecond;
    private static long sRecordMaxTime = 60 * 60 * 1000;
    private static final SimpleDateFormat sDurationTimerFormat = new SimpleDateFormat("mm:ss");
    private Runnable mTimestampRunnable = new Runnable() {
        @Override
        public void run() {
            if (StartVideoRecordActivity.this.isFinishing()) {
                return;
            }
            mTalkTimeSecond++;
            int progress = (int) (((float) (mTalkTimeSecond * 1000) / sRecordMaxTime) * 100);
            updateTimestamp(progress);
            if (progress < 100) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimestamp(int progress) {
        String time = sDurationTimerFormat.format(mTalkTimeSecond * 1000);
        timer.setVisibility(View.VISIBLE);
        timer.setText(time);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        if (null == videoRecorderManager.getCamera()) {
            preview.removeAllViews();
            videoRecorderManager.setCameraInstance(currentCameraFacing);
            preview.addView(videoRecorderManager.getPreview(preview));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, " keyCode " + keyCode);
        if (keyCode == 26 || keyCode == 4) {
            if (isRecording){
                showDialog();
            }else {
                closeCamera();
                this.finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setMessage("是否开启后台录制")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(StartVideoRecordActivity.this, VideoRecorderServer.class);
                        startService(intent);
                        finish();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeCamera();
                        finish();
                    }
                }).create();
//        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.setCanceledOnTouchOutside(false);//点击屏幕不消失
        alertDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_movieRecorder:
                try {
                    if (isRecording) {
                        handler.removeCallbacks(stopRecordRunable);
                        handler.removeCallbacks(startRecordRunable);
                        handler.postDelayed(stopRecordRunable, 300);
                    } else {
                        // initialize video camera
                        handler.removeCallbacks(startRecordRunable);
                        handler.removeCallbacks(stopRecordRunable);
                        handler.postDelayed(startRecordRunable, 300);
                    }
                } catch (Exception e) {

                }
                break;
            case R.id.iv_switch_camera:
//                startActivity(new Intent(this,UnionVideoRecoderActivity.class));
                Log.e(TAG, "iv_switch_camera click");
                try {
                    if (!isRecording) {
                        if (null != videoRecorderManager.getCamera()) {
                            videoRecorderManager.releaseCamera();
                            videoRecorderManager.releaseMediaRecorder();
                        }
                        if (currentCameraFacing == 1) {
                            videoRecorderManager.setCameraInstance(0);
                            PreferLogin.putIccid(this, 0);
                        } else if (currentCameraFacing == 0) {
                            videoRecorderManager.setCameraInstance(1);
                            PreferLogin.putIccid(this, 1);
                        }
                        try {
                            videoRecorderManager.getCamera().setPreviewDisplay(videoRecorderManager.getPreview().getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        videoRecorderManager.getCamera().startPreview();
                    }
                } catch (Exception e) {

                }

                break;
            case R.id.video_list:
                startActivity(new Intent(this, VideoFileListActivity.class));
                closeCamera();
                break;
            case R.id.iv_flash:
                try {
                    if (null != videoRecorderManager.getCamera()) {
                        Camera.Parameters parameters = videoRecorderManager.getCamera().getParameters();
                        if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        } else {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        }
                        videoRecorderManager.getCamera().setParameters(parameters);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

        }
    }

    Runnable startRecordRunable = new Runnable() {
        @Override
        public void run() {
            if (videoRecorderManager.prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                videoRecorderManager.getMediaRecorder().start();
                handler.post(mTimestampRunnable);
                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                videoRecorderManager.releaseMediaRecorder();
                // inform user
            }
        }
    };
    Runnable stopRecordRunable = new Runnable() {
        @Override
        public void run() {
            // stop recording and release camera
            videoRecorderManager.getMediaRecorder().stop();  // stop the recording
            videoRecorderManager.releaseMediaRecorder(); // release the MediaRecorder object
            videoRecorderManager.getCamera().lock();         // take camera access back from MediaRecorder
            handler.removeCallbacks(mTimestampRunnable);
            timer.setVisibility(View.GONE);
            mTalkTimeSecond = 0;
            isRecording = false;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                //申请权限，REQUEST_TAKE_PHOTO_PERMISSION是自定义的常量
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        0);
            } else if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        0);
            } else if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        0);
            } else if (ContextCompat.checkSelfPermission(StartVideoRecordActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        0);
            } else {
                Log.e(TAG, "PERMISSION_GRANTED   " + Camera.getNumberOfCameras());
                preview.removeAllViews();
                currentCameraFacing = PreferLogin.getIccid(this, 1);
                videoRecorderManager.setCameraInstance(currentCameraFacing);
                preview.addView(videoRecorderManager.getPreview(preview));
            }
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void startVideoRecord(StartVideoRecordEvent event) {
//        Log.e(TAG, isRecording + "  1111");
//        if (isRecording) {
//            handler.removeCallbacks(stopRecordRunable);
//            handler.removeCallbacks(startRecordRunable);
//            handler.postDelayed(stopRecordRunable, 300);
//        } else {
//            // initialize video camera
//            handler.removeCallbacks(startRecordRunable);
//            handler.removeCallbacks(stopRecordRunable);
//            handler.postDelayed(startRecordRunable, 300);
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void switchCamera(SwitchCameraEvent event) {
        if (null != videoRecorderManager.getCamera() && !isRecording) {
            Log.e(TAG, "iv_switch_camera click");
            if (null != videoRecorderManager.getCamera()) {
                videoRecorderManager.releaseCamera();
                videoRecorderManager.releaseMediaRecorder();
            }
            if (currentCameraFacing == 1) {
                videoRecorderManager.setCameraInstance(0);
                PreferLogin.putIccid(this, 0);
            } else if (currentCameraFacing == 0) {
                videoRecorderManager.setCameraInstance(1);
                PreferLogin.putIccid(this, 1);
            }
            try {
                videoRecorderManager.getCamera().setPreviewDisplay(videoRecorderManager.getPreview().getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
            videoRecorderManager.getCamera().startPreview();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop");

    }

    private void closeCamera() {
        handler.removeCallbacks(mTimestampRunnable);
        timer.setVisibility(View.GONE);
        mTalkTimeSecond = 0;
        videoRecorderManager.releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        videoRecorderManager.releaseCamera();              // release the camera immediately on pause event
        preview.removeAllViews();
        isRecording = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
//        isRecording = false;
//        closeCamera();
        EventBus.getDefault().unregister(this);
    }
}

