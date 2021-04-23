package com.example.camerademo6.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.appcompat.widget.AppCompatImageView;

import com.example.camerademo6.R;

//圆形和圆角图片 AppCompatImageView封装
public class RoundImageView extends AppCompatImageView {
    /**
     * 图片的类型，圆形or圆角
     */
    private int type;
    private static final int TYPE_CIRCLE = 0;
    public static final int TYPE_ROUND = 1;
    private static final String STATE_INSTANCE = "state_instance";
    private static final String STATE_TYPE = "state_type";
    private static final String STATE_BORDER_RADIUS = "state_border_radius";

    /**
     * 圆角大小的默认值
     */
    private static final int BODER_RADIUS_DEFAULT = 10;
    /**
     * 圆角的大小
     */
    private int mBorderRadius;

    /**
     * 绘图的Paint
     */
    private Paint mBitmapPaint;
    /**
     * 圆角的半径
     */
    private int mRadius;
    /**
     * 3x3 矩阵，主要用于缩小放大
     */
    private Matrix mMatrix;
    /**
     * 渲染图像，使用图像为绘制图形着色
     */
    private BitmapShader mBitmapShader;
    private Bitmap bmp;

    private Paint mRingPaint;
    /**
     * view的宽度
     */
    private int mWidth;
    private RectF mRoundRect;


    public void setBitmap(Bitmap bitmap) {
        bmp = bitmap;
        invalidate();
    }

    public RoundImageView(Context context) {
        super(context);
    }

    /*
    * 构造函数：
    * 实例化缩放大小的矩阵
    * 设置位图的画笔-------->缩略图
    * 设置圆环的画笔-------->旋转摄像头？*/
    public RoundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMatrix = new Matrix();
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);

        mRingPaint = new Paint();
        mRingPaint.setColor(Color.WHITE);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(2);

        //bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);

        // // 获取自定属性配置
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RoundImageView);
        //R.styleable.RoundImageView会报红---->在values的attrs.xml中进行声明？

        mBorderRadius = a.getDimensionPixelSize(
                R.styleable.RoundImageView_borderRadius, (int) TypedValue
                        .applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                BODER_RADIUS_DEFAULT, getResources()
                                        .getDisplayMetrics()));// 默认为10dp
        type = a.getInt(R.styleable.RoundImageView_type, TYPE_CIRCLE);// 默认为Circle

        a.recycle();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        /**
         * 如果类型是圆形，则强制改变view的宽高一致，以小值为准
         */
        if (type == TYPE_CIRCLE) {
            mWidth = Math.min(getMeasuredWidth(), getMeasuredHeight());
            mRadius = mWidth / 2;
            setMeasuredDimension(mWidth, mWidth);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (bmp == null) return;

        setUpShader();

        if (type == TYPE_ROUND) {
            //画圆角....此处不走
            canvas.drawRoundRect(mRoundRect, mBorderRadius, mBorderRadius,
                    mBitmapPaint);
        } else {
            //圆形--->绘制缩略图
            canvas.drawCircle(mRadius, mRadius, mRadius, mBitmapPaint);
            //画边线
            canvas.drawCircle(mRadius, mRadius, mRadius - 1, mRingPaint);
        }
    }








    /*
    * Shader在三维软件中称之为着色器，就是用来给空白图形上色用的
    *               给Shader指定对应的图像、渐变色等来填充图形*/
    private void setUpShader() {

        //bitmap用来指定图案，
        // tileX用来指定当X轴超出单个图片大小时时所使用的重复策略，取值有：
                                                    //TileMode.CLAMP:用边缘色彩填充多余空间
                                                    //TileMode.REPEAT:重复原图像来填充多余空间
                                                    //TileMode.MIRROR:重复使用镜像模式的图像来填充多余空间

        mBitmapShader = new BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = 1.0f;
        if (type == TYPE_CIRCLE) {
            // 拿到bitmap宽或高的小值
            int bSize = Math.min(bmp.getWidth(), bmp.getHeight());
            //缩放
            scale = mWidth * 1.0f / bSize;

        } else if (type == TYPE_ROUND) {
            // 如果图片的宽或者高与view的宽高不匹配，计算出需要缩放的比例；
            // 缩放后的图片的宽高，一定要大于我们view的宽高；所以我们这里取大值；
            scale = Math.max(getWidth() * 1.0f / bmp.getWidth(), getHeight()
                    * 1.0f / bmp.getHeight());
        }
        // shader的变换矩阵，我们这里主要用于放大或者缩小
        mMatrix.setScale(scale, scale);
        // 设置变换矩阵
        mBitmapShader.setLocalMatrix(mMatrix);
        // 给画笔设置shader
        mBitmapPaint.setShader(mBitmapShader);
    }



}
