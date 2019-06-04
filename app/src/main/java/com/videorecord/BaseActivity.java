package com.videorecord;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by wuwentao on 2019/3/11.
 */

public class BaseActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication.getInstance().addActivity(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        MyApplication.getInstance().setCurrentActivity(this);
    }

    @Override
    public void finish() {
        super.finish();

        MyApplication.getInstance().removeActivity(this);
        MyApplication.getInstance().setCurrentActivity(null);
    }
}
