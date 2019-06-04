package com.videorecord;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wuwentao on 2019/2/28.
 */

public class VideoFileListActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    ImageView back_btn;
    ListView video_list_view;
    TextView edit_btn;
    TextView delete_btn,select_all_btn;
    LinearLayout edit_layout;
    List<VideoFileStatus> videoList = new ArrayList();
    VideoItemAdapter videoItemAdapter;
    boolean isEdit;
    boolean isSelectAll;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_file_list_activity);
        EventBus.getDefault().register(this);
        back_btn = (ImageView) findViewById(R.id.back_btn);
        video_list_view  = (ListView) findViewById(R.id.video_list_view);

        videoItemAdapter = new VideoItemAdapter(this,videoList);
        video_list_view.setAdapter(videoItemAdapter);
        video_list_view.setOnItemClickListener(this);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        handler.sendEmptyMessageDelayed(1000,200);
        edit_layout = (LinearLayout) findViewById(R.id.edit_layout);
        edit_btn = (TextView) findViewById(R.id.edit_btn);
        edit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isEdit){
                    isEdit = true;
                    edit_btn.setText("完成");
                    videoItemAdapter.setEdit(true);
                    edit_layout.setVisibility(View.VISIBLE);
                    videoItemAdapter.notifyDataSetChanged();
                }else {
                    isEdit = false;
                    edit_btn.setText("编辑");
                    videoItemAdapter.setEdit(false);
                    edit_layout.setVisibility(View.GONE);
                    videoItemAdapter.notifyDataSetChanged();
                }
            }
        });
        delete_btn = (TextView) findViewById(R.id.delete_btn);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isDeleteSuccess = false;
                for (VideoFileStatus videoFileStatus:videoList){
                    if (videoFileStatus.isCheck) {
                        isDeleteSuccess = true;
                        FileUtils.RecursionDeleteFile(new File(videoFileStatus.filePath));
                    }
                }
                if (!isDeleteSuccess){
                    return;
                }
                Toast.makeText(VideoFileListActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                handler.sendEmptyMessageDelayed(1000,200);
            }
        });

        select_all_btn = (TextView) findViewById(R.id.select_all_btn);
        select_all_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSelectAll){
                    isSelectAll = true;
                    select_all_btn.setText("全不选");
                }else {
                    isSelectAll = false;
                    select_all_btn.setText("全选");
                }
                for (VideoFileStatus videoFileStatus:videoList){
                    videoFileStatus.isCheck = isSelectAll;
                }
                videoItemAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this,VideoPlayActivity.class);
        intent.putExtra("url",videoList.get(position).filePath);
        startActivity(intent);
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FileUtils.clearLisr();
//            videoList = FileUtils.searchMp4Infos(new File(FileUtils.getBaseCachePath()),new String[]{".mp4",".3gp"});
            if (FileUtils.isCanRead){
                videoList.addAll(FileUtils.searchMp4Infos(new File(FileUtils.getExternalStoragePath()),new String[]{".mp4",".3gp"}));
            }
            videoItemAdapter.notifyData(videoList);
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void startRecord(NeedStartRecordEvent event) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
