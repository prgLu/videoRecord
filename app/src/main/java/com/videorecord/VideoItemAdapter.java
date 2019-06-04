package com.videorecord;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wuwentao on 2019/3/1.
 */

public class VideoItemAdapter extends BaseAdapter{

    List<VideoFileStatus> videoList = new ArrayList();
    Context context;
    boolean isEdit;
    public VideoItemAdapter(Context context,List<VideoFileStatus> videoList){
        this.context = context;
        this.videoList = videoList;
    }

    public void notifyData(List<VideoFileStatus> videoList){
        this.videoList = videoList;
        notifyDataSetChanged();
    }

    public void setEdit(boolean edit) {
        isEdit = edit;
    }

    @Override
    public int getCount() {
        return videoList.size();
    }

    @Override
    public Object getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHold viewHold;
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.video_list_item_layout,null);
            viewHold = new ViewHold();
            viewHold.fileName = convertView.findViewById(R.id.file_name);
//            viewHold.edit_check = convertView.findViewById(R.id.edit_check);
            convertView.setTag(viewHold);
        }else {
            viewHold = (ViewHold) convertView.getTag();
        }
        viewHold.fileName.setText(FileUtils.getFileName(videoList.get(position).filePath));
//        if (isEdit){
//            viewHold.edit_check.setVisibility(View.VISIBLE);
//        }else {
//            viewHold.edit_check.setVisibility(View.GONE);
//        }
//        viewHold.edit_check.setChecked(videoList.get(position).isCheck);
//        viewHold.edit_check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    videoList.get(position).isCheck=isChecked;
//            }
//        });
        return convertView;
    }

    class ViewHold{
        TextView fileName;
        CheckBox edit_check;
    }
}
