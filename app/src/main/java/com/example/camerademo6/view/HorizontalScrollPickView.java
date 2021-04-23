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
    private int mTouchSlop;//判定为拖动的最小移动像素数.判断当前用户操作是否是滑动
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

    /*适配器addview?**/
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
    public interface SelectListener {
        void onSelect(int beforePosition, int position);
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
        mScroller = new Scroller(context, new DecelerateInterpolator());


        setOrientation(LinearLayout.HORIZONTAL);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();//获取移动像素

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
        int childCount = getChildCount();
        int childLeft;
        int childRight;
        int selectedMode = mSelectedIndex;
        int widthOffset = 0;
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
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
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
                if (!mIsDoAction && absDeltaX > mTouchSlop && absDeltaX > absDeltaY) {
                    mIsDoAction = true;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
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


    //向左滑动
    public void moveLeft() {
        moveToPoint(mSelectedIndex - 1);
    }

    //向右滑动
    public void moveRight() {
        moveToPoint(mSelectedIndex + 1);
    }

    private void moveToPoint(int index) {
        if (mAdapter == null) {
            return;
        }
        if (index < 0 || index >= mAdapter.getCount() || index == mSelectedIndex) {
            return;
        }
        mBeforeIndex = mSelectedIndex;
        View toView = getChildAt(index);
        int[] screens = new int[2];
        toView.getLocationOnScreen(screens);
        int moveSize = Math.round((screens[0] + mWidths[index] / 2.0F) - mHalfScreenSize);
        if (ApiHelper.getScreenMode(getContext()) == ApiHelper.SCREEN_MODE_FULL) {
            /*
            * 借助Scroller来完成后续的滚动操作。
            * 先根据当前的滚动位置来计算布局应该继续滚动到哪一个子控件的页面，然后计算出距离该页面还需滚动多少距离。
            * 接下来调用startScroll()方法来初始化滚动数据并刷新界面。
            * startScroll()方法第一个参数是滚动开始时X的坐标，第二个参数是滚动开始时Y的坐标，第三个参数是横向滚动的距离，正值表示向左滚动，第四个参数是纵向滚动的距离，正值表示向上滚动。
            * 紧接着调用invalidate()方法来刷新界面*/
            mScroller.startScroll(getScrollX(), 0, moveSize, 0, mDuration);
        } else {
            if (ApiHelper.getScreenActive(getContext()) == ApiHelper.SCREEN_ACTIVE_MAIN) {
                mScroller.startScroll(getScrollX(), 0, moveSize, 0, mDuration);
            } else {
                mScroller.startScroll(getScrollX(), 0, moveSize, 0, mDuration);
            }
        }

        scrollToNext(mBeforeIndex, index);
        mSelectedIndex = index;
        invalidate();
    }
    /*
    * 适配器设置前一个view
    * 适配器设置当前view
    * 监听事件
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


    private void initChildView(View view) {
        if (mAdapter == null || view == null) {
            return;
        }
        mAdapter.initView(view);
    }
    private void selectView(View view) {
        if (mAdapter == null || view == null) {
            return;
        }
        mAdapter.selectView(view);
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
