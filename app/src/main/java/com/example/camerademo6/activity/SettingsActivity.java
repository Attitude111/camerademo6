package com.example.camerademo6.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.example.camerademo6.R;
import com.example.camerademo6.cam.CameraConstant;
import com.example.camerademo6.storage.SharedPreferencesController;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener{
    private ImageView mBack;
    private Switch mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestFullScreenActivity();
        setContentView(R.layout.activity_settings);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.water_mark_switch:
                SharedPreferencesController.getInstance(this).spPutBoolean(CameraConstant.ADD_WATER_MARK, isChecked);
                break;
            /*case R.id.focus_take_picture:
                SharedPreferencesController.getInstance(this).spPutBoolean(CameraConstant.FOCUS_TAKE_PICTURE, isChecked);
                break;*/
        }
    }

    /*初始化
    * 找到返回对应的view
    * 找到添加水印的switch
    * 为两个view设监听
    * 更新switch的状态（根据sp中的值）
    * */
    private void initView() {
        mBack = findViewById(R.id.back);
        mSwitch = findViewById(R.id.water_mark_switch);
        //mFocusTakePic = findViewById(R.id.focus_take_picture);
        mBack.setOnClickListener(this);
        mSwitch.setOnCheckedChangeListener(this);
       // mFocusTakePic.setOnCheckedChangeListener(this);
        boolean addMark = SharedPreferencesController.getInstance(this).spGetBoolean(CameraConstant.ADD_WATER_MARK);//为false
        //boolean focusPicture = SharedPreferencesController.getInstance(this).spGetBoolean(CameraConstant.FOCUS_TAKE_PICTURE);
        mSwitch.setChecked(addMark);
        //mFocusTakePic.setChecked(focusPicture);
    }



    private void requestFullScreenActivity() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}