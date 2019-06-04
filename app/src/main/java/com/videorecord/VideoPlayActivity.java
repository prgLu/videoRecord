package com.videorecord;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class VideoPlayActivity extends BaseActivity {

    UniversalVideoView mVideoView;
    UniversalMediaController mMediaController;
    FrameLayout mVideoLayout;

    private static final String TAG = "VideoPlayActivity";
    private static final String SEEK_POSITION_KEY = "SEEK_POSITION_KEY";
    private String VIDEO_URL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
    private int mSeekPosition;
    private int cachedHeight;
    private boolean isFullscreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play_layout);

        VIDEO_URL = getIntent().getStringExtra("url");
        mVideoView = (UniversalVideoView) findViewById(R.id.videoView);
        mMediaController = (UniversalMediaController) findViewById(R.id.media_controller);
        mVideoLayout = (FrameLayout) findViewById(R.id.video_layout);
        setVideoAreaSize();
        mMediaController.setTitle(FileUtils.getFileName(VIDEO_URL));
        mVideoView.setVideoViewCallback(new UniversalVideoView.VideoViewCallback() {

            @Override
            public void onScaleChange(boolean isFullscreen) {
                VideoPlayActivity.this.isFullscreen = isFullscreen;
                if (isFullscreen) {
                    ViewGroup.LayoutParams layoutParams = mVideoLayout.getLayoutParams();
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    mVideoLayout.setLayoutParams(layoutParams);

                } else {
                    ViewGroup.LayoutParams layoutParams = mVideoLayout.getLayoutParams();
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    mVideoLayout.setLayoutParams(layoutParams);
                }
            }

            @Override
            public void onPause(MediaPlayer mediaPlayer) {
                //Toast.makeText(VideoPlayActivity.this, "pause", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStart(MediaPlayer mediaPlayer) {
                //Toast.makeText(VideoPlayActivity.this, "onStart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBufferingStart(MediaPlayer mediaPlayer) {
                //Toast.makeText(VideoPlayActivity.this, "onBufferingStart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBufferingEnd(MediaPlayer mediaPlayer) {
                //Toast.makeText(VideoPlayActivity.this, "onBufferingEnd", Toast.LENGTH_SHORT).show();
            }
        });
        mVideoView.setMediaController(mMediaController);
        mVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            mSeekPosition = mVideoView.getCurrentPosition();
            Log.d(TAG, "onPause mSeekPosition=" + mSeekPosition);
            mVideoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null && !mVideoView.isPlaying()) {
            mVideoView.seekTo(mSeekPosition);
            mVideoView.start();
        }
    }


    private void setVideoAreaSize() {
        mVideoLayout.post(new Runnable() {
            @Override
            public void run() {
                int width = mVideoLayout.getWidth();
                cachedHeight = (int) (width * 405f / 720f);
                RelativeLayout.LayoutParams videoLayoutParams = (RelativeLayout.LayoutParams) mVideoLayout.getLayoutParams();
                videoLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                videoLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                videoLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                mVideoLayout.setLayoutParams(videoLayoutParams);
                mVideoView.setVideoPath(VIDEO_URL);
                mVideoView.requestFocus();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState Position=" + mVideoView.getCurrentPosition());
        outState.putInt(SEEK_POSITION_KEY, mSeekPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle outState) {
        super.onRestoreInstanceState(outState);
        mSeekPosition = outState.getInt(SEEK_POSITION_KEY);
        Log.d(TAG, "onRestoreInstanceState Position=" + mSeekPosition);
    }

    @Override
    public void onBackPressed() {
        if (this.isFullscreen) {
            mVideoView.setFullscreen(false);
        } else {
            super.onBackPressed();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void startRecord(NeedStartRecordEvent event) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
