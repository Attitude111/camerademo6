package com.example.camerademo6.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AutoFitTextureView extends TextureView {

    //宽高比
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(@NonNull Context context) {
        super(context);
    }

    public AutoFitTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        //super(context, attrs);
        this(context,attrs,0);
    }
    //最后都走这一个？
    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     *给出设定宽高比的方法，重新测量？
     *  Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();//onMeasure onLayout
        //invalidate();onDraw主綫程
        //postInvalidate();onDraw子綫程
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //利用MeasureSpec提取宽高值
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        //当宽或高的比例未指定时，父类传过来的建议宽度和高度值就是view的宽高
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        }
        //若指定了宽高比，即宽高比都不为0
        else
            {

            if (width < height * mRatioWidth / mRatioHeight) {
                //如果宽高比小于指定宽高（宽度小或高度大），修改高度，宽度仍然为父类建议宽度
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                //若宽高比大于指定宽高（宽度大或高度小），修改宽度，高度仍为父类建议高度
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }



}
