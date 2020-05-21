package com.yezh.mydraglistdemo.View;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

/**
 * 效果分析：
 * 1、只有列表可以拖动
 * 2、垂直拖动的范围只能是后面菜单view 的高度
 * 3、松开手时，要么关闭，要么打开
 * 4.加上listView以后，ListView可以滑动，但是但是菜单拖动没有效果了
 * 原因：ListView中调用了requestDisallowInterceptTouchEvent(true)  请求父类（VerticalDragListView）不要拦截
 */
public class VerticalDragListView extends FrameLayout {
    // 可以认为这是系统给我们写好的一个工具类
    private ViewDragHelper mViewDragHelper;
    private View mListView;
    private int mMenuHeight;
    private boolean mMenuIsOpen = false;


    public VerticalDragListView(Context context) {
        this(context, null);
    }

    public VerticalDragListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d("TAG", "VerticalDragListView");
        mViewDragHelper = ViewDragHelper.create(this, mDragHelperCallback);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.d("TAG", "height" + getMeasuredHeight() + " width = " + getMeasuredWidth());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new RuntimeException("VerticalDragListView 只能包含两个子View");
        }
        mListView = getChildAt(1);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            View menuView = getChildAt(0);
            mMenuHeight = menuView.getMeasuredHeight();
            Log.d("TAG", "mMenuHeight = " + mMenuHeight);
        }
    }

    private float mDowmY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //菜单打开的时候要全部拦截
        if (mMenuIsOpen) {
            return true;
        }

        //向下拖动的时候拦截，不要给ListView处理
        //但是子类可以调用requestDisallowInterceptTouch请求父类不要拦截，其实就是改变的mGroupFlags的值
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //按下的时候获取滑动的Y值
                mDowmY = ev.getY();
                //让DragHelper拿到完整的事件
                mViewDragHelper.processTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = ev.getY();
                if ((moveY - mDowmY) > 0 && !canChildScrollUp()) {
                    //向下滑动不让listView处理
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
//         Log.e("TAG", "onTouchEvent -> " + event.getAction());
        return true;
    }

    // 1.拖动我们的子View
    private ViewDragHelper.Callback mDragHelperCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            //指定该子view---child 是否可以拖动
            //2.只有列表可以拖动
            return mListView == child;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            // 垂直拖动移动的位置
            //列表 拖动范围只能是菜单的高度
            if (top < 0) {
                top = 0;
            }

            if (top > mMenuHeight) {
                top = mMenuHeight;
            }
            return top;
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            //松开手的是时候 没有拖动
            super.onViewReleased(releasedChild, xvel, yvel);
            if (mListView.getTop() > mMenuHeight / 2) {
                //拖动超过菜单高度的一半的时候，展开列表
                mViewDragHelper.settleCapturedViewAt(0, mMenuHeight);
                mMenuIsOpen = true;
            } else {
                //否则拖动到零的位置
                mViewDragHelper.settleCapturedViewAt(0, 0);
                mMenuIsOpen = false;
            }
            invalidate();

        }
    };

    /**
     * 响应滚动
     */
    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     * 判断View是否滚动到了最顶部,还能不能向上滚
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mListView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mListView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mListView, -1) || mListView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mListView, -1);
        }
    }
}
