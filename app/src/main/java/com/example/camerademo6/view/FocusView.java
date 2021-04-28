package com.example.camerademo6.view;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.camerademo6.utils.DensityUtils;

public class FocusView extends View {

    private Paint mPaint;
    private float mStrokeWidth = 4.0f;

    private int mInnerRadiusDP = 7;
    public static final int mOuterRadiusDP = 40;
    private int mInnerRadiusPX;//转换成px的内圆半径
    private int mOuterRadiusPX;

    private boolean mNeedToDrawView;
    //指示需要画图位置的坐标
    private float mViewCenterX;
    private float mViewCenterY;


    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initData(context);//初始化
    }


    /*
    * 画笔实例化
    * 设置画笔
    * dp换为px*/
    private void initData(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);

        mInnerRadiusPX = DensityUtils.dip2px(context,mInnerRadiusDP);
        mOuterRadiusPX = DensityUtils.dip2px(context,mOuterRadiusDP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //判断是否需要draw
        if(mNeedToDrawView){
            //在view的中心位置（鼠标点击位置）画两个圆
            canvas.drawCircle(mViewCenterX, mViewCenterY, mOuterRadiusPX, mPaint);
            canvas.drawCircle(mViewCenterX, mViewCenterY, mInnerRadiusPX, mPaint);
        }
    }

    //设置是否需要draw并invalidate
    public void setNeedToDrawView(boolean b) {
        mNeedToDrawView = b;
        invalidate();
    }
    //设置focus的位置并invalidate-->可见则ondraw方法
    public void setFocusViewCenter(float x, float y) {
        mViewCenterX = x;
        mViewCenterY = y;
        invalidate();
    }
    /*focusview的动画效果*/
    public void playAnimation() {
        //内圆动画
        ValueAnimator animIner = ValueAnimator.ofFloat(mInnerRadiusPX, mInnerRadiusPX-5, mInnerRadiusPX);
        animIner.setDuration(500);

        animIner.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue();
                mInnerRadiusPX = (int) currentValue;
                invalidate();
            }
        });

        //外圆动画
        ValueAnimator animOuter = ValueAnimator.ofFloat(mOuterRadiusPX, mOuterRadiusPX+10, mOuterRadiusPX);
        animOuter.setDuration(500);

        animOuter.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currentValue = (float) animation.getAnimatedValue();
                mOuterRadiusPX = (int) currentValue;
                invalidate();//根据当前动画值改变半径
            }
        });

        AnimatorSet set =new AnimatorSet();
        set.playTogether(animIner,animOuter);//通过AnimatorSet将动画一起播放
        set.start();//开始动画
    }
}
