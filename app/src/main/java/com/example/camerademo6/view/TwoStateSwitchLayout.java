package com.example.camerademo6.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.camerademo6.R;

public class TwoStateSwitchLayout extends FrameLayout {

    private ImageView ivCheckOn, ivCheckOff;// 两种状态对应的ImageView
    private CustomCheckBoxChangeListener customCheckBoxChangeListener;// 切换的监听器
    private boolean isCheck;// 是否被选中的标志值

    /**
     * 获取CustomCheckBox的选择状态
     */
    public boolean isCheck() {
        return isCheck;
    }


    public TwoStateSwitchLayout(@NonNull Context context) {
        super(context);
    }

    public TwoStateSwitchLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // 自定义一个ViewGroup，需要获取这个view的布局
        // 设置布局文件  只有root不等于空的情况下才能够真正的把view添加到listView中,root表示view的容器是什么
        /*
        * LayoutInflater的作用类似于findViewById()，
        * 不同点是LayoutInflater是用来找layout下xml布局文件，并且实例化
        * 而findViewById()是找具体xml下的具体widget控件
        * 对于一个没有被载入或者想要动态载入的界面，都需要使用inflate来载入
        * 我们启动一个应用，与入口Activity相关的layout{常见的是main.xml}就是被载入的
        * */
        LayoutInflater.from(context).inflate(R.layout.flash_switch, this);




        // 获取控件元素
        ivCheckOn = (ImageView) findViewById(R.id.view_flash_on);
        ivCheckOff = (ImageView) findViewById(R.id.view_flash_off);

        // 设置两个ImageView的点击事件
        ivCheckOn.setOnClickListener(new ClickListener());
        ivCheckOff.setOnClickListener(new ClickListener());

        // 读取xml中设置的资源属性ID---->拿
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.TwoStateSwitch);
        int imageOnResId = array.getResourceId(R.styleable.TwoStateSwitch_imageOn, -1);
        int imageOffResId = array.getResourceId(R.styleable.TwoStateSwitch_imageOff, -1);

        // 设置开启状态和关闭状态显示的图片：R.styleable.TwoStateSwitch_imageOn
        setOnImage(imageOnResId);
        setOffImage(imageOffResId);

        // 对象回收
        array.recycle();

        // 默认显示的是没被选中的状态
        setCheckOff();
    }

    /**
     * 设置开启状态时CustomCheckBox的图片
     *
     * @param resId
     *            图片资源ID
     */
    public void setOnImage(int resId) {
        ivCheckOn.setImageResource(resId);
    }
    /**
     * 设置关闭状态时CustomCheckBox的图片
     *
     * @param resId
     *            图片资源ID
     */
    public void setOffImage(int resId) {

        ivCheckOff.setImageResource(resId);
    }
    /**
     * 设置CustomCheckBox为未被选中的状态
     * isCheck置为false
     * 对应的选中的图片设置为不可见
     *       未选中的图片设置为可见
     */
    public void setCheckOff() {
        isCheck = false;
        ivCheckOn.setVisibility(GONE);
        ivCheckOff.setVisibility(VISIBLE);
    }
    /**
     * 设置CustomCheckBox为选中状态
     */
    public void setCheckOn() {
        isCheck = true;
        ivCheckOn.setVisibility(VISIBLE);
        ivCheckOff.setVisibility(GONE);
    }


    /**
     * 自定义CustomCheckBox中控件的事件监听器
     */
    private class ClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.view_flash_on:
                    setCheckOff();//设置CustomCheckBox为未被选中的状态
                    customCheckBoxChangeListener.customCheckBoxOff(getId());//自定义选中监听事件
                    break;
                case R.id.view_flash_off:
                    setCheckOn();
                    customCheckBoxChangeListener.customCheckBoxOn(getId());
                    break;
            }
        }
    }

    /**
     * 状态改变监听接口
     */
    public interface CustomCheckBoxChangeListener {
        void customCheckBoxOn(int flashSwitch);

        void customCheckBoxOff(int flashSwitch);
    }

    /**
     * 为CustomCheckBox设置监听器
     *
     * @param customCheckBoxChangeListener
     *            监听器接口对象
     */
    public void setCustomCheckBoxChangeListener(
            CustomCheckBoxChangeListener customCheckBoxChangeListener) {
        this.customCheckBoxChangeListener = customCheckBoxChangeListener;
    }


}
