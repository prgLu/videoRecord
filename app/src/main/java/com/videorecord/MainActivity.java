package com.videorecord;


import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static String TAG = "MainActivity";
    private FrameLayout preview;
    private ImageView ivRecordingSwitch, ivFlash, ivCameraSwitch, video_list;
    private TextView tvComplete, timer;
    MediaRecorder mMediaRecorder;
    Camera mCamera;
    CameraPreview mPreview;
    static boolean isRecording;
    int currentCameraFacing = 0;//0后置，1前置
    String currentVideoFile="";
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
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //申请权限，REQUEST_TAKE_PHOTO_PERMISSION是自定义的常量
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    0);
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0);
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    0);
        } else {
            Log.e(TAG, "PERMISSION_GRANTED   " + Camera.getNumberOfCameras());
            currentCameraFacing = PreferLogin.getIccid(this, 0);
            mCamera = getCameraInstance(currentCameraFacing);
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview);
        }
        if (getIntent().getBooleanExtra("isStartRecord", false)) {
            handler.removeCallbacks(startRecordRunable);
            handler.removeCallbacks(stopRecordRunable);
            handler.postDelayed(startRecordRunable, 500);
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
                if (prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mMediaRecorder.start();
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
            if (MainActivity.this.isFinishing()) {
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
        if (null == mCamera) {
            preview.removeAllViews();
            mCamera = getCameraInstance(currentCameraFacing);
            mPreview = new CameraPreview(this, mCamera);
            preview.addView(mPreview);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, " keyCode " + keyCode);
        if (keyCode == 26) {
//            if (isRecording){
//                handler.removeCallbacks(stopRecordRunable);
//                handler.removeCallbacks(startRecordRunable);
//                handler.postDelayed(stopRecordRunable,300);
//            }
            this.finish();
        }
        return super.onKeyDown(keyCode, event);
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
                        if (null != mCamera) {
                            releaseCamera();
                            releaseMediaRecorder();
                        }
                        if (currentCameraFacing == 1) {
                            mCamera = getCameraInstance(0);
                            PreferLogin.putIccid(this, 0);
                        } else if (currentCameraFacing == 0) {
                            mCamera = getCameraInstance(1);
                            PreferLogin.putIccid(this, 1);
                        }
                        try {
                            mCamera.setPreviewDisplay(mPreview.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mCamera.startPreview();
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
                    if (null != mCamera) {
                        Camera.Parameters parameters = mCamera.getParameters();
                        if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        } else {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        }
                        mCamera.setParameters(parameters);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }

        }
    }

    Runnable startRecordRunable = new Runnable() {
        @Override
        public void run() {
            try {
                if (prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mMediaRecorder.start();
                    handler.post(mTimestampRunnable);
                    isRecording = true;
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder();
                    // inform user
                }
            }catch (Exception e){
                isRecording = true;
                FileUtils.deleteSingleFile(currentVideoFile);
                releaseMediaRecorder();
            }
        }
    };
    Runnable stopRecordRunable = new Runnable() {
        @Override
        public void run() {
            // stop recording and release camera
            if (mMediaRecorder != null) {
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                try {
                    mMediaRecorder.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    FileUtils.deleteSingleFile(currentVideoFile);
                } catch (RuntimeException e) {
                    FileUtils.deleteSingleFile(currentVideoFile);
                    e.printStackTrace();
                } catch (Exception e) {
                    FileUtils.deleteSingleFile(currentVideoFile);
                    e.printStackTrace();
                }
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder
            handler.removeCallbacks(mTimestampRunnable);
            timer.setVisibility(View.GONE);
            mTalkTimeSecond = 0;
            isRecording = false;
        }
    };

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // 通知摄像头可以在这里绘制预览了
            if (mCamera == null) {
                Log.e(TAG, "camera is null");
                return;
            }
            try {
                Log.d(TAG, "setPreviewDisplay setPreviewDisplay ");
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // 什么都不做，但是在Activity中Camera要正确地释放预览视图
            mHolder.removeCallback(this);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // 如果预览视图可变或者旋转，要在这里处理好这些事件
            // 在重置大小或格式化时，确保停止预览

            if (mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            // 变更之前要停止预览
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // 在这里重置预览视图的大小、旋转、格式化

            // 使用新设置启动预览视图
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

    private Camera getCameraInstance(int cameraId) {
        if (null == mCamera) {
            int frontIndex = -1;
            int backIndex = -1;
            int cameraCount = Camera.getNumberOfCameras();
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
                Camera.getCameraInfo(cameraIndex, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Log.e(TAG, "frontIndex == " + cameraIndex);
                    frontIndex = cameraIndex;
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Log.e(TAG, "backIndex == " + cameraIndex);
                    backIndex = cameraIndex;
                }
            }
            currentCameraFacing = cameraId;
            Camera c = null;
            try {
                if (cameraId == 1 && frontIndex != -1) {
                    c = Camera.open(frontIndex);
                } else if (cameraId == 0 && backIndex != -1) {
                    c = Camera.open(backIndex);
                }
                c.setDisplayOrientation(90);
                return c;
            } catch (Exception e) {
                // Camera被占用或者设备上没有相机时会崩溃。
                Log.e(TAG, e.getMessage());
            }
        }
        return mCamera;
    }

    private boolean prepareVideoRecorder() {


        try {
            mCamera = getCameraInstance(currentCameraFacing);
            mMediaRecorder = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            // Step 2: Set sources
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            if (currentCameraFacing == 1) {
                mMediaRecorder.setOrientationHint(270);
            } else {
                mMediaRecorder.setOrientationHint(90);
            }
            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            // 设置音频的编码格式
            mMediaRecorder.setProfile(getBestProfile());

            // Step 4: Set output file
            currentVideoFile = FileUtils.getOutputMediaFile(FileUtils.getBaseCachePath());
            mMediaRecorder.setOutputFile(currentVideoFile);

            // Step 5: Set the preview output
            mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

            // Step 6: Prepare configured MediaRecorder
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (RuntimeException e) {
            Log.d(TAG, "RuntimeException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private CamcorderProfile getBestProfile() {

        CamcorderProfile profile;
        profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        //FIXME In case video width not correct when 480P
        if (profile.videoFrameWidth == 720 && profile.videoFrameHeight == 480) {
            profile.videoFrameWidth = 640;
            //profile.videoFrameRate = 20;
        }
        Log.e("UnionVideoRecoderActivity", "####  lidp 480p profile quality=" + profile.quality + "  " + profile.videoFrameWidth + "x" + profile.videoFrameHeight
                + " biterate " + profile.videoBitRate + "  framerate " + profile.videoFrameRate + " codec " + profile.videoCodec
                + " Build.MODEL=" + Build.MODEL + " setSize=");


        profile.videoCodec = MediaRecorder.VideoEncoder.H264;

        profile.videoBitRate = profile.videoBitRate / 2;
        //profile.videoCodec = "";
        return profile;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            Log.e(TAG, "mMediaRecorder");
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            Log.e(TAG, "releaseCamera");
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                //申请权限，REQUEST_TAKE_PHOTO_PERMISSION是自定义的常量
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        0);
            } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        0);
            } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        0);
            } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        0);
            } else {
                Log.e(TAG, "PERMISSION_GRANTED   " + Camera.getNumberOfCameras());
                preview.removeAllViews();
                currentCameraFacing = PreferLogin.getIccid(this, 1);
                mCamera = getCameraInstance(currentCameraFacing);
                mPreview = new CameraPreview(this, mCamera);
                preview.addView(mPreview);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void startVideoRecord(StartVideoRecordEvent event) {
        Log.e(TAG, isRecording + "  1111");
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
//        if (isRecording) {
//            // stop recording and release camera
//            Toast.makeText(MyApplication.getInstance().getApplicationContext(),"stop Recording ",Toast.LENGTH_SHORT).show();
//            if (null == mMediaRecorder) {
//                Log.e(TAG, " mMediaRecorder null 1111");
//            }
//            mMediaRecorder.stop();  // stop the recording
//            releaseMediaRecorder(); // release the MediaRecorder object
//            handler.removeCallbacks(mTimestampRunnable);
//            timer.setVisibility(View.GONE);
//            mTalkTimeSecond = 0;
//            isRecording = false;
//        } else {
//            Toast.makeText(MyApplication.getInstance().getApplicationContext(),"start Recording",Toast.LENGTH_SHORT).show();
//            if (prepareVideoRecorder()) {
//                // Camera is available and unlocked, MediaRecorder is prepared,
//                // now you can start recording
//                mMediaRecorder.start();
//                handler.post(mTimestampRunnable);
//                isRecording = true;
//            }
//        }
    }

    boolean isNeedRecord;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void switchCamera(PTTDownEvent event) {
        if (event.isDown) {
            if (isRecording) {
                isNeedRecord = true;
                handler.removeCallbacks(startRecordRunable);
                handler.post(stopRecordRunable);
            }
        } else {
            if (isNeedRecord) {
                isNeedRecord = false;
                handler.removeCallbacks(startRecordRunable);
                handler.removeCallbacks(stopRecordRunable);
                handler.postDelayed(startRecordRunable, 300);
            }
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
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        preview.removeView(mPreview);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        isRecording = false;
        closeCamera();
        EventBus.getDefault().unregister(this);
    }
}
