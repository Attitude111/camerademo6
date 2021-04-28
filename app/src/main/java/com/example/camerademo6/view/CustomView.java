package com.example.camerademo6.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/*
*
*
* onMeasure()：测量自己的大小，为正式布局提供建议。（注意，只是建议，至于用不用，要看onLayout）;
* onLayout():使用layout()函数对所有子控件布局；
* onDraw():根据布局的位置绘图；**/




public class CustomView extends View {

    private Paint mPaint;
    public CustomView(Context context) {
        super(context);
    }

    /*
    *
    * 构造方法中添加一个AttributeSet类型的参数来解析控件属性
    * */
    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint();//初始化画笔
    }



    /**

     * 初始化画笔

     */

    private void initPaint() {

        // 实例化画笔并打开抗锯齿
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        /*设置画笔样式为描边，
        * 画笔样式分三种：
        *   1.Paint.Style.STROKE：描边
        *   2.Paint.Style.FILL_AND_STROKE：描边并填充
        *   3.Paint.Style.FILL：填充
        */
        mPaint.setStyle(Paint.Style.STROKE);
        // 设置画笔颜色为浅灰色
        mPaint.setColor(Color.LTGRAY);
        /*设置描边的粗细，单位：像素px
        * 注意：当setStrokeWidth(0)的时候描边宽度并不为0而是只占一个像素
        */
        mPaint.setStrokeWidth(10);


    }

    /*
     * 在onDraw方法中，画布Canvas作为参数被传递进来，也就是说这个画布是Android为我们准备好的，不需要去管（当然你也可以自定义一张画布在上面绘制自己的东西并将其传递给父类）
     *       实例化Paint对象并设置属性（setAntiAlias(true)：一种让图像边缘显得更圆滑光泽动感的碉堡算法）
     *                                //不建议在draw或者layout的过程中去实例化对象！因为draw或layout的过程有可能是一个频繁重复执行的过程
     * */
    @Override
    protected void onDraw(Canvas canvas) {


        super.onDraw(canvas);
/*

        // 在onDraw方法中绘制圆环  drawCircle（圆心x的坐标，圆心y坐标，圆半径，画笔）
        canvas.drawCircle(
                //工具类MeasureUtil获取屏幕尺寸--->
                MeasureUtil.getScreenSize((Activity) mContext)[0] / 2,
                MeasureUtil.getScreenSize((Activity) mContext)[1] / 2,
                200,
                mPaint);

*/


        //Android中提供了一个叫invalidate()的方法来让我们重绘我们的View
        // postInvalidate()；用它替代我们原来的invalidate()即可非UI线程中更新UI
    }

    /*
     * onMeasure()的作用就是根据container内部的子控件计算自己的宽和高，
     * 最后通过setMeasuredDimension（int width,int height设置进去）*/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //是从父类传过来的建议宽度和高度值：widthMeasureSpec、heightMeasureSpec
        //利用MeasureSpec提取宽高值
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        /*
         * 利用MeasureSpec提取对应的模式
         * wrap_content-> MeasureSpec.AT_MOST
         * match_parent -> MeasureSpec.EXACTLY
         * 具体值 -> MeasureSpec.EXACTLY
         */
        int measureWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        //测量它所有的子控件


       /*
       int height = 0;
        int width = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            //测量子控件
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            //获得子控件的高度和宽度
            int childHeight = child.getMeasuredHeight();
            int childWidth = child.getMeasuredWidth();
            //得到最大宽度，并且累加高度
            height += childHeight;
            width = Math.max(childWidth, width);
        }
        */


        //wrap_content or match_parent
       ////////////// setMeasuredDimension((measureWidthMode == MeasureSpec.EXACTLY) ? measureWidth : width, (measureHeightMode == MeasureSpec.EXACTLY) ? measureHeight : height);

    }

    /*
     *onLayout():对所有子控件布局
     * setMeasuredDimension()提供的测量结果只是为布局提供建议，最终的取用与否要看layout()函数
      */


    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int top = 0;

        /*
        int count = getChildCount();
        for (int i=0;i<count;i++) {
            View child = getChildAt(i);

            int childHeight = child.getMeasuredHeight();
            int childWidth = child.getMeasuredWidth();
            //调用layout()函数设置子控件所在的位置
            *//*将所有的控件垂直排列
                     top指的是控件的顶点，
                     bottom的坐标就是top+childHeight,
                     从最左边开始布局，那么right的坐标就是子控件的宽度值childWidth.  顺时针参数？*//*
            child.layout(0, top, childWidth, top + childHeight);
            top += childHeight;
        }
        */


    }

    /*1. getMeasuredWidth()与getWidth()
    * 他们的值大部分时间都是相同的，但意义确是根本不一样的，
    * 区别主要体现在下面几点：
    * - 首先getMeasureWidth()方法在measure()过程结束后就可以获取到了，
    *   而getWidth()方法要在layout()过程结束后才能获取到。
    * - getMeasureWidth()方法中的值是通过setMeasuredDimension()方法来进行设置的，
    *  而getWidth()方法中的值则是通过layout(left,top,right,bottom)方法设置的。

    * */

    /*2.container自己什么时候被布局
    * 在它布局里，会调用它自己的一个layout()函数(不能被重载，代码位于View.java)，在SetFrame(l,t,r,b)就是设置自己的位置
    * 设置结束以后才会调用onLayout(changed, l, t, r, b)来设置内部所有子控件的位置。
     * */

    /*3.如何得到自定义控件的左右间距margin值
    *https://blog.csdn.net/harvic880925/article/details/47029169
    *   如果我们在onLayout()中根据margin来布局的话，
    *   那么我们在onMeasure()中计算container的大小时，也要加上margin，
    *   不然会导致container太小，而控件显示不全的问题
    *
    *  */

}
