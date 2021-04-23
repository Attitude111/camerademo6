package com.example.camerademo6.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.camerademo6.R;
import com.example.camerademo6.cam.CameraConstant;

public class ShutterButton extends View {
    private Paint mInnerFillCircle;
    private Paint mOuterRing;

    private int mCurrentMode = CameraConstant.PHOTO_MODE;
    //view的高宽
    private int mWidth;
    private int mHeight;

    private int mRadius;

    private boolean mVideoRecordState;
    private RectF mRectVideoRecording;

    public OnShutterButtonClickLister mLister;//自定义点击监听

    //引用
    public void setOnShutterButtonClickListener(OnShutterButtonClickLister listener) {
        mLister = listener;
    }
    public void setCurrentMode(int currentMode) {
        mCurrentMode = currentMode;
        invalidate();
    }

    public void setVideoRecordingState(boolean recording) {
        mVideoRecordState = recording;
        invalidate();
    }

    public ShutterButton(Context context) {
        super(context);
    }

    /*构造时初始化两只画笔*/
    public ShutterButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mInnerFillCircle = new Paint();
        mInnerFillCircle.setColor(getResources().getColor(R.color.circle_color_thumbnail));
        mInnerFillCircle.setAntiAlias(true);
        mInnerFillCircle.setStyle(Paint.Style.FILL);

        mOuterRing = new Paint();
        mOuterRing.setColor(getResources().getColor(R.color.circle_color_thumbnail));
        mOuterRing.setAntiAlias(true);
        mOuterRing.setStyle(Paint.Style.STROKE);
        mOuterRing.setStrokeWidth(3.0f);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        /*根据当前模式作画*/
        if (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_FPS_MODE) {
            mInnerFillCircle.setColor(Color.RED);
        } else if (mCurrentMode == CameraConstant.PHOTO_MODE || mCurrentMode == CameraConstant.PRO_MODE) {
            mInnerFillCircle.setColor(Color.WHITE);
        }

        if (mVideoRecordState && (mCurrentMode == CameraConstant.VIDEO_MODE || mCurrentMode == CameraConstant.SLOW_FPS_MODE)) {
            canvas.drawRoundRect(mRectVideoRecording, 10.0f, 10.0f, mInnerFillCircle);
        } else {
            canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mInnerFillCircle);
        }
        canvas.drawCircle(mWidth / 2, mHeight / 2, mWidth / 2 - 3, mOuterRing);
    }

    /**/
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        mRadius = mWidth / 2 - 20;
        mRectVideoRecording = new RectF();
        mRectVideoRecording.left = mWidth / 4;
        mRectVideoRecording.right = mWidth * 3 / 4;
        mRectVideoRecording.top = mHeight / 4;
        mRectVideoRecording.bottom = mHeight * 3 / 4;
    }
    /*在view里面
    由于没有注册onTouch，去执行onTouchEvent(event)--->点击快门*/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (mLister != null) {
                    mLister.onShutterButtonClick(mCurrentMode);
                }
                break;
        }
        return true;//管当前的action是什么，最终都一定返回一个true 使得下面的action得以执行
    }

    public interface OnShutterButtonClickLister {
        void onShutterButtonClick(int mode);
    }


    /*按下快门的动画？？*/
    public void startPictureAnimator() {
        //Property Animator(属性动画)通过改变控件内部的属性值来达到动画效果的
        //第一步：创建ValueAnimator实例：对指定的数字区间进行动画运算
        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(mWidth / 2 - 20, mWidth / 2 - 10, mWidth / 2 - 20);
        valueAnimator.setDuration(800);
        //第二步：添加监听：对运算过程进行监听，然后自己对控件做动画操作
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mRadius = (int) value;
                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

            }
        });
        valueAnimator.start();
    }


}
