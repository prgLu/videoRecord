package com.videorecord;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Created by wuwentao on 2019/3/30.
 */

public class VideoRecorderManager {
    private String TAG="VideoRecorderManager";
    private static VideoRecorderManager instance;
    private int currentCameraFacing;
    MediaRecorder mMediaRecorder;
    CameraPreview mPreview;
    Camera mCamera;
    Context context;
    FrameLayout mCameraPreview;
    public static VideoRecorderManager getInstance(Context context) {
        if (null == instance) {
            instance = new VideoRecorderManager(context);
        }
        return instance;
    }

    private VideoRecorderManager(Context context){
        this.context = context;
    }

    public Camera getCamera(){
        return mCamera;
    }

    public MediaRecorder getMediaRecorder(){
        return mMediaRecorder;
    }

    public CameraPreview getPreview(FrameLayout view){
        if (null == mPreview){
            mPreview = new CameraPreview(context,mCamera);
        }
        if (null != mPreview.getParent()){
            Log.e("FSDFASDFS","mPreview.getParent() != NULL");
            mCameraPreview.removeAllViews();
        }
        mCameraPreview= view;
        return mPreview;
    }

    public CameraPreview getPreview(){
        if (null == mPreview){
            mPreview = new CameraPreview(context,mCamera);
        }
        return mPreview;
    }

    public Camera setCameraInstance(int cameraId) {
        if (null == mCamera || cameraId != currentCameraFacing) {
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
                mCamera = c;
                return c;
            } catch (Exception e) {
                // Camera被占用或者设备上没有相机时会崩溃。
                Log.e(TAG, e.getMessage());
            }
        }
        return mCamera;
    }


    public boolean prepareVideoRecorder() {

        mCamera = setCameraInstance(currentCameraFacing);
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (currentCameraFacing == 1) {
            mMediaRecorder.setOrientationHint(270);
        }else {
            mMediaRecorder.setOrientationHint(90);
        }
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        // 设置音频的编码格式
        mMediaRecorder.setProfile(getBestProfile());

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(FileUtils.getOutputMediaFile(FileUtils.getBaseCachePath()));

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
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
            Log.e(TAG,"surfaceCreated surfaceCreated");
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
            Log.e(TAG,"surfaceDestroyed surfaceDestroyed");
//            mHolder.removeCallback(this);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // 如果预览视图可变或者旋转，要在这里处理好这些事件
            // 在重置大小或格式化时，确保停止预览
            Log.e(TAG,"surfaceChanged surfaceChanged");
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


    public void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            Log.e(TAG, "mMediaRecorder");
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            Log.e(TAG, "releaseCamera");
            mPreview = null;
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

}
