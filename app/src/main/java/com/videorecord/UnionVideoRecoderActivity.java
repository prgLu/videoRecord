package com.videorecord;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cameraview.CameraView;


/**
 * Created by xubin on 16-10-28.
 */
public class UnionVideoRecoderActivity extends Activity{


    enum State {
        IDLE, RECORDING, WAIT_SURE, TAKING_PIC
    }

    CameraView mCameraPreview;
    TextView mTimerTv;
    ImageView iv_movieRecorder;

    private MediaRecorder mRecorder;
    private Handler mHandler = new Handler();
    private Handler mBackgroundHandler;
    private String mOutputFilePath;
    private long mTalkTimeSecond;
    private static final SimpleDateFormat sDurationTimerFormat = new SimpleDateFormat("mm:ss");
    private long mHandleButtonDownTime;
    private long mHandleButtonIsVideoActionThreshold = 300;
    private static long sRecordMaxTime = 2 * 60 * 1000;
    private State mState = State.IDLE;
    private MediaPlayer mMediaPlayer;
    private boolean hasPermission;
    private int mSettingWidth;
    private int mSettingHeight;

    private boolean mAutoRepeat;//重复录制
    private boolean mAutoVideo;
    private boolean mAutoPic;
    private boolean mHandFree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!hasCamera(this)) {
            finish();
            return;
        }
//        hasPermission = checkCameraPermission() && checkAudioRecordPermission();

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_recoder);

        mCameraPreview = findViewById(R.id.camera_preview);
        mTimerTv = findViewById(R.id.timer);
        iv_movieRecorder = findViewById(R.id.iv_movieRecorder);
        mCameraPreview.addCallback(mCallback);
        mCameraPreview.setKeepScreenOn(true);
        initTouchButton();

        mCameraPreview.setPictureQuality(100);
        registerReceiver(mReleaseCameraReceiver, new IntentFilter("action_Release_Camera"));

    }

    private void initTouchButton() {
        iv_movieRecorder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (mState != State.RECORDING) {
                        removeAllAutoRecordAction();
                        mHandleButtonDownTime = System.currentTimeMillis();
                        mHandler.removeCallbacks(mCheckLongClickConform);
                        mHandler.postDelayed(mCheckLongClickConform, mHandleButtonIsVideoActionThreshold);
                    }else {
                        performVideoAction(false);
                    }
            }
        });

    }

    private void removeAllAutoRecordAction() {
        if (mAutoRepeat) {
            // 防止手动介入触摸事件后，还存在自动录制的delay action.
            mHandler.removeCallbacksAndMessages(null);
        }
    }


    private Runnable mCheckLongClickConform = new Runnable() {
        @Override
        public void run() {
            performVideoAction(true);
        }
    };

    private void performVideoAction(boolean start) {
        Log.e("UnionVideoRecoderActivity","performVideoAction start=" + start);
        if (start) {
            boolean ret = startRecording();
            if (ret) {
                mState = State.RECORDING;
            } else {
                mState = State.IDLE;
            }
            Log.e("UnionVideoRecoderActivity","startRecording " + (mState == State.RECORDING));
        } else {
            if (mState == State.RECORDING) {
                mState = State.WAIT_SURE;
                mAutoVideo = false;
                releaseRecording();
                //触发下一次自动录制
            } else {
                performPicAction();
            }
        }
    }

    private void performPicAction() {
        Log.e("UnionVideoRecoderActivity","performPicAction start");
        mState = State.TAKING_PIC;
        try {
            mCameraPreview.takePicture();
        } catch (Exception e) {
            mState = State.IDLE;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mCameraPreview.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("UnionVideoRecoderActivity","onResume, isCameraOpened=" + mCameraPreview.isCameraOpened());

            try {
                mCameraPreview.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        mState = State.IDLE;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
        releasePlay();
        if (mCameraPreview.getCamera() != null) {
            ((Camera) mCameraPreview.getCamera()).release();
        }
        unregisterReceiver(mReleaseCameraReceiver);
    }

    private boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void updateCameraRotation() {
        if (mCameraPreview != null && mCameraPreview.getCamera() != null) {
            Camera camera = (Camera) mCameraPreview.getCamera();
            Camera.Parameters parameters = camera.getParameters();
            parameters.setRotation(mCameraPreview.getFacing() == CameraView.FACING_FRONT ? 270 : 0);
            camera.setParameters(parameters);
        }
    }

    private boolean checkCameraFacing(final int facing) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            return false;
        }
        final int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (facing == info.facing) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFrontFacingCamera() {
        final int CAMERA_FACING_BACK = 1;
        return checkCameraFacing(CAMERA_FACING_BACK);
    }

    @Override
    public void onBackPressed() {
        if (mAutoRepeat) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onBackPressed();
    }

    private CamcorderProfile getBestProfile() {

        CamcorderProfile profile;
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            //FIXME In case video width not correct when 480P
            if (profile == CamcorderProfile.get(CamcorderProfile.QUALITY_480P)) {
                profile.videoFrameWidth = 640;
                profile.videoFrameRate = 20;
            }
        Log.e("UnionVideoRecoderActivity","####  lidp 480p profile quality=" + profile.quality + "  " + profile.videoFrameWidth + "x" + profile.videoFrameHeight
                + " biterate " + profile.videoBitRate + "  framerate " + profile.videoFrameRate + " codec " + profile.videoCodec
                + " Build.MODEL=" + Build.MODEL + " setSize=" );


        profile.videoCodec = MediaRecorder.VideoEncoder.H264;

        profile.videoBitRate = profile.videoBitRate / 2;
        //profile.videoCodec = "";
        return profile;
    }

    private void checkMediaRecorder() {
        if (mCameraPreview.getCamera() == null) {
            Toast.makeText(getApplicationContext(), "camera is null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            if (mCameraPreview.getCamera() != null) {
                ((Camera) mCameraPreview.getCamera()).unlock();
                mRecorder.setCamera((Camera) mCameraPreview.getCamera());
            }
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            CamcorderProfile profile = getBestProfile();
            mRecorder.setProfile(profile);

            if (mCameraPreview.getFacing() == CameraView.FACING_FRONT) {
                mRecorder.setOrientationHint(270);
            } else {
                mRecorder.setOrientationHint(90);
            }
            final String last = mOutputFilePath;
            if (!mAutoRepeat) {
                getBackgroundHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        removeLastOutputFile(last);
                    }
                });
            }
            mOutputFilePath = getOutputMediaFile();
            mRecorder.setOutputFile(mOutputFilePath);
            Log.e("UnionVideoRecoderActivity","#### start recode video -> " + mOutputFilePath);
        } else {
            if (mCameraPreview.getCamera() != null) {
                ((Camera) mCameraPreview.getCamera()).unlock();
                mRecorder.setCamera((Camera) mCameraPreview.getCamera());
            }
        }

        mRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.e("UnionVideoRecoderActivity",("MediaRecorder.onError " + what));
            }
        });
        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                Log.e("UnionVideoRecoderActivity",("MediaRecorder.onInfo " + what));
            }
        });
    }

    private String getOutputMediaFile() {
        String path = Environment.getExternalStorageDirectory().getPath();
        File file = new File(path + "/recorder_video/");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getAbsolutePath() + "/" + getSystemTime() + ".mp4";
    }

    /**
     * 获得当前系统时间
     */
    public static String getSystemTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
        String format = simpleDateFormat.format(date);
        return format;
    }

    private void removeLastOutputFile(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private boolean startRecording() {
        Log.e("UnionVideoRecoderActivity","startRecording");
        try {
            checkMediaRecorder();
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder = null;
        }

        if (mRecorder != null) {
            try {
                mRecorder.prepare();
                mRecorder.start();
                return true;
            } catch (Exception e) {
                releaseRecording();
                Log.e("UnionVideoRecoderActivity","startRecording exception=" + e);
            }
        }
        return false;
    }

    private void releaseRecording() {
        if (mCameraPreview.isCameraOpened() && mCameraPreview.getCamera() != null) {
            ((Camera) mCameraPreview.getCamera()).lock();
        }
        if (mRecorder != null) {
            try {
                mRecorder.setOnErrorListener(null);
                mRecorder.setOnInfoListener(null);
                mRecorder.setPreviewDisplay(null);
                mRecorder.reset();
                mRecorder.release();
            } catch (Exception e) {
                Log.e("UnionVideoRecoderActivity","releaseRecording exception");
            } finally {
                mRecorder = null;
            }
        }
    }


    private void updateTimestamp(int progress) {
        String time = sDurationTimerFormat.format(mTalkTimeSecond * 1000);
        mTimerTv.setText(time);
    }

    private void setRecoderResult() {
        if (TextUtils.isEmpty(mOutputFilePath) || !new File(mOutputFilePath).exists()) {
            setResult(RESULT_CANCELED);
        } else {
            Intent intent = new Intent();
            intent.putExtra("path", mOutputFilePath);
            intent.putExtra("time", mTalkTimeSecond);
            intent.putExtra("resolution", mSettingHeight + "x" + mSettingWidth);
            setResult(RESULT_OK, intent);
        }
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void play() {
        File file = new File(mOutputFilePath);
        if (!file.exists()) {
            Toast.makeText(this, "video error, please retry", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e("UnionVideoRecoderActivity","play video " + file.getAbsolutePath() + " size=" + file.length());
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(file.getAbsolutePath());
            mMediaPlayer.setSurface(mCameraPreview.getSurface());
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    replay();
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("UnionVideoRecoderActivity","video play error " + extra);
                    releasePlay();
                    mState = State.IDLE;
                    mCameraPreview.start();
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releasePlay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * 重新开始播放
     */
    protected void replay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
        } else {
            play();
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.e("UnionVideoRecoderActivity","onCameraOpened");

        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.e("UnionVideoRecoderActivity","onCameraClosed");

        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {

        }
    };


    private final BroadcastReceiver mReleaseCameraReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UnionVideoRecoderActivity.this.finish();
        }
    };
}
