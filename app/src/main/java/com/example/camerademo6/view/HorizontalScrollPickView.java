package com.example.camerademo6.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import com.example.camerademo6.utils.ApiHelper;

/*自定义滚动选择器*/
public class HorizontalScrollPickView extends LinearLayout {

    private static final String TAG = "HorizoPickView";
    private static final int INVALID_POINTER = -1;//无效的选择位

    private Context mContext;
    private Scroller mScroller;//有可能用户只是将布局拖动到了中间，不能让布局就这么停留在中间的位置，因此接下来就需要借助Scroller来完成后续的滚动操作

    private int mBeforeIndex;
    private int mSelectedIndex = 0;//选中的位置，默认为0

    private int mDuration = 320;//动画持续时间
    private int mTouchSlop;//系统 滑动距离的最小值，大于该值可以认为滑动
     /*
     记录了用户手指按下时的X坐标位置，以及用户手指在屏幕上拖动时的X坐标位置，
     当两者之间的距离大于TouchSlop值时，就认为用户正在拖动布局，
     然后我们就将事件在这里拦截掉，阻止事件传递到子控件当中
     当我们把事件拦截掉之后，就会将事件交给ScrollerLayout的onTouchEvent()方法来处理
    */

    /*
    * 上次触发事件时的屏幕坐标
    * */
    private int mLastMotionX;
    private int mLastMotionY;

    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDoAction = false;
    private SelectListener mSelectListener;//选择监听
    private int mHalfScreenSize;///一半横屏的大小？？？
    private Integer[] mWidths;//记录每个子控件的宽度
    boolean mLayoutSuccess = false;
    private PickAdapter mAdapter;
    private boolean isTouch = false;

    /*适配器**/
    public void setAdapter(PickAdapter adapter) {
        this.mAdapter = adapter;
        if (this.mAdapter == null) {
            return;
        }
        mWidths = new Integer[this.mAdapter.getCount()];
        addViews();
    }
    public void setDefaultSelectedIndex(int selectedIndex) {
        this.mSelectedIndex = selectedIndex;
    }

    public void setSelectListener(SelectListener selectListener) {
        this.mSelectListener = selectListener;
    }


    public HorizontalScrollPickView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //初始化控件
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "HorizontalScrollPickView init");
        mContext = context;
        /*https://blog.csdn.net/guolin_blog/article/details/48719871
        * Scroller的基本用法主要可以分为以下几个步骤：
           创建Scroller的实例
           调用startScroll()方法来初始化滚动数据并刷新界面
           重写computeScroll()方法，并在其内部完成平滑滚动的逻辑*/
        mScroller = new Scroller(context, new DecelerateInterpolator());//其动画速率开始较快，后面减速


        setOrientation(LinearLayout.HORIZONTAL);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();//获取touchSlop （系统 滑动距离的最小值，大于该值可以认为滑动）

        Point displaySize = new Point();//坐标
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(displaySize);
        mHalfScreenSize = displaySize.x / 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mLayoutSuccess) {
            return;
        }
        mLayoutSuccess = true;
        int childCount = getChildCount();//viewgroup中的
        int childLeft;
        int childRight;
        int selectedMode = mSelectedIndex;
        int widthOffset = 0;//offset抵消、补偿
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (i < selectedMode) {
                widthOffset += childView.getMeasuredWidth();//widthOffset选中项view之前所有的view的宽度和
            }
        }

        for (int i = 0; i < childCount; i++) {
            /*
            *为每一个子view放置位置*/
            View childView = getChildAt(i);
            mWidths[i] = childView.getMeasuredWidth();
            if (i != 0) {

                View preView = getChildAt(i - 1);
                childLeft = preView.getRight();
                childRight = childLeft + childView.getMeasuredWidth();
            } else {
                childLeft = (getWidth() - getChildAt(selectedMode).getMeasuredWidth()) / 2 - widthOffset;
                childRight = childLeft + childView.getMeasuredWidth();
            }
            childView.layout(childLeft, childView.getTop(), childRight, childView.getMeasuredHeight());
            initChildView(childView);//每个子view设置适配器
        }

        selectView(getChildAt(selectedMode));//当前选中view
    }

    /*处理滑动事件*/
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return scrollEvent(ev) || super.dispatchTouchEvent(ev);
    }

    /*
    * 点击了某个控件，首先会去调用该控件所在布局的dispatchTouchEvent方法
    * 然后在布局的dispatchTouchEvent方法中找到被点击的相应控件，再去调用该控件的dispatchTouchEvent方法(也就是此处的scrollEvent  ontouch或ontouchevent)*/
    private boolean scrollEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                isTouch = true;
                mLastMotionX = (int) ev.getX();//返回这个事件的x坐标 for给定 指针索引
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);//查找此索引的指针标识符
                mIsDoAction = false;
                return !super.dispatchTouchEvent(ev);//返回super.dispatchTouchEvent(ev)的非
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }

                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = mLastMotionX - x;
                int deltaY = mLastMotionY - y;
                int absDeltaX = Math.abs(deltaX);
                int absDeltaY = Math.abs(deltaY);
                if (!mIsDoAction && absDeltaX > mTouchSlop && absDeltaX > absDeltaY) {//判断是否移动
                    mIsDoAction = true;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);//不中断
                    }
                    if (deltaX > 0) {
                        moveRight();//向右滑动
                    } else {
                        moveLeft();//向左滑动
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER;
                isTouch = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
                isTouch = false;
                break;
        }

        return mIsDoAction;//默认是false 不能继续触发系列事件
    }


    //向左滑动：当前选择的下标减一
    public void moveLeft() {
        moveToPoint(mSelectedIndex - 1);
    }

    //向右滑动，当前选择的下标加一
    public void moveRight() {
        moveToPoint(mSelectedIndex + 1);
    }

    /*移动到指定坐标 ，传递参数为指定坐标 */
    private void moveToPoint(int index) {
        if (mAdapter == null) {
            return;
        }
        if (index < 0 || index >= mAdapter.getCount() || index == mSelectedIndex) {
            return;
        }
        mBeforeIndex = mSelectedIndex;//选中坐标成为上一个坐标
        View toView = getChildAt(index);//拿到选中view
        int[] screens = new int[2];
        toView.getLocationOnScreen(screens);//拿到选中view在屏幕中的位置
        int moveSize = Math.round((screens[0] + mWidths[index] / 2.0F) - mHalfScreenSize);//取整，移动距离. 避免距离移动到一半
        if (ApiHelper.getScreenMode(getContext()) == ApiHelper.SCREEN_MODE_FULL) {//全屏
            /*
            * 借助Scroller来完成后续的滚动操作。
            * 调用startScroll()方法来初始化滚动数据并刷新界面。
            * startScroll()方法第一个参数是滚动开始时X的坐标，第二个参数是滚动开始时Y的坐标，第三个参数是横向滚动的距离，正值表示向左滚动，第四个参数是纵向滚动的距离，正值表示向上滚动。第五个动画持续时间
            *
            *
            * 紧接着调用invalidate()方法来刷新界面*/
            mScroller.startScroll(getScrollX(), 0, moveSize, 0, mDuration);
        } else {
            if (ApiHelper.getScreenActive(getContext()) == ApiHelper.SCREEN_ACTIVE_MAIN) {//主屏
                mScroller.startScroll(getScrollX(), 0, moveSize, 0, mDuration);
            } else {
                mScroller.startScroll(getScrollX(), 0, moveSize, 0, mDuration);
            }
        }

        scrollToNext(mBeforeIndex, index);//更新view 这里是颜色改变
        mSelectedIndex = index;//更新当前选中的坐标
        invalidate();
    }
    /*
    * 更新前一个选中view
    * 更新当前选中view
    * 如果有监听 则触发监听事件
    * */
    private void scrollToNext(int lastIndex, int selectIndex) {
        Log.d(TAG, "HorizontalScrollPickView scrollToNext");
        if (mAdapter == null) {
            return;
        }
        View preView = getChildAt(lastIndex);
        if (preView != null) {
            mAdapter.preView(preView);
        }
        View selectView = getChildAt(selectIndex);
        if (selectView != null) {
            mAdapter.selectView(selectView);
        }

        if (mSelectListener != null) {
            mSelectListener.onSelect(lastIndex, selectIndex);
        }
    }


    /*如果有适配器，使用适配器改变当前view*/
    private void initChildView(View view) {
        if (mAdapter == null || view == null) {
            return;
        }
        mAdapter.initView(view);
    }
    /*如果有适配器，使用适配器改变当前view为选中view*/
    private void selectView(View view) {
        if (mAdapter == null || view == null) {
            return;
        }
        mAdapter.selectView(view);
    }

    public interface SelectListener {
        void onSelect(int beforePosition, int position);
    }

    public static abstract class PickAdapter {
        public abstract int getCount();

        public abstract View getPositionView(int position, ViewGroup parent, LayoutInflater inflater);

        public void initView(View view) {
        }

        public void selectView(View view) {
        }

        public void preView(View view) {
        }
    }

    private void addViews() {
        Log.d(TAG, "HorizontalScrollPickView addViews");
        if (mAdapter == null) {
            return;
        }
        for (int i = 0; i < mAdapter.getCount(); i++) {
            View view = mAdapter.getPositionView(i, this, LayoutInflater.from(mContext));
            final int index = i;
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "HorizontalScrollPickView onClick");
                    moveToPoint(index);
                }
            });
            addView(view);
        }
    }

}
