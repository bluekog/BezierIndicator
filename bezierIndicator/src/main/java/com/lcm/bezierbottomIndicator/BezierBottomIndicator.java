package com.lcm.bezierbottomIndicator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * ****************************************************************
 * Author: LCM
 * Date: 2017/7/17 下午7:34
 * Desc:
 * *****************************************************************
 */

public class BezierBottomIndicator extends ViewGroup {

    private static final String TAG = "BezierBottomIndicator";

    private boolean isAnimatorStart = false;
    private boolean isViewPagerScoll = false;

    private int width = 0;
    private int height = 0;

    private int childSideLength = 0;  //子view外框的边长
    private float childPadding = 20;  //子View的Padding值

    private float defaultLeftRightGap = 10; //左右两边默认的距离

    private int bgCircularColor = Color.parseColor("#aaaaaa");


    private List<PointF> anchorList;
    private Paint childBgPaint;

    private Paint bezierPaint;
    private BezierCircular bezierCircular;

    private int currentPosition = 0;
    boolean direction = true;
    int targetPosition = 0;

    private ViewPager viewPager;

    public BezierBottomIndicator(Context context) {
        this(context, null);
    }

    public BezierBottomIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BezierBottomIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BezierBottomIndicator, defStyleAttr, 0);
        int indexCount = typedArray.getIndexCount();

        for (int i = 0; i < indexCount; i++) {
            int attr = typedArray.getIndex(i);

            if (attr == R.styleable.BezierBottomIndicator_leftRightGap) {
                defaultLeftRightGap = typedArray.getDimension(attr, 10);
            } else if (attr == R.styleable.BezierBottomIndicator_childPadding) {
                childPadding = typedArray.getDimension(attr, 20);
            } else if (attr == R.styleable.BezierBottomIndicator_bgCircularColor) {
                bgCircularColor = typedArray.getColor(attr, bgCircularColor);
            }
        }
        typedArray.recycle();

    }


    private void init() {
        //自定义ViewGroup默认不会调用OnDraw()方法
        setWillNotDraw(false);

        anchorList = new ArrayList<>();

        childBgPaint = new Paint();
        childBgPaint.setColor(bgCircularColor);
        childBgPaint.setAntiAlias(true);
        childBgPaint.setStrokeWidth(2);
        childBgPaint.setStyle(Paint.Style.STROKE);


        bezierPaint = new Paint();
        bezierPaint.setColor(Color.parseColor("#FF4081"));
        bezierPaint.setAntiAlias(true);
        bezierPaint.setStyle(Paint.Style.FILL);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        /**
         * 获得此ViewGroup上级容器为其推荐的宽和高，以及计算模式
         */
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = sizeWidth;
        } else {
            width = wm.getDefaultDisplay().getWidth();
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = sizeHeight;
        } else {
            height = wm.getDefaultDisplay().getHeight();
        }

        if (getChildCount() != 0) {
            childSideLength = (width - getPaddingRight() - getPaddingLeft()) / getChildCount() > height - getPaddingBottom() - getPaddingTop() ? height - getPaddingBottom() - getPaddingTop() : (width - getPaddingLeft() - getPaddingRight()) / getChildCount();
//        //计算出所有的ChildView的宽和高
            measureChildren(widthMeasureSpec, heightMeasureSpec);
            bezierCircular = new BezierCircular(childSideLength / 2);
        }

        setMeasuredDimension(width, height);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        if (childCount == 0) {
            return;
        }

        float childDis = (width - getPaddingLeft() - getPaddingRight() - 2 * defaultLeftRightGap - childSideLength) / (childCount - 1);

        float cWidth = childSideLength - 2 * childPadding;
        float cHeight = cWidth;

        anchorList.clear();
        //计算子控件的位置，强制将子View控制绘制在均分的几个锚点上
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            PointF anchorPoint = new PointF((childDis * i + defaultLeftRightGap + childSideLength / 2 + getPaddingLeft()), getPaddingTop() + childSideLength / 2);
            anchorList.add(anchorPoint);
            childView.layout((int) (anchorPoint.x - cWidth / 2), (int) (anchorPoint.y - cHeight / 2), (int) (anchorPoint.x + cWidth / 2), (int) (anchorPoint.y + cHeight / 2));
        }
        PointF pointF = anchorList.get(0);
        bezierCircular.setCenter(pointF.x, pointF.y);
        bezierCircular.initControlPoint();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        bezierCircular.drawCircle(canvas, bezierPaint);
        drawChildBg(canvas);
        super.onDraw(canvas);
    }


    float touchX = 0;
    float touchY = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                touchY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                Log.i(TAG, "touchX: " + touchX + "  touchY: " + touchY);
                for (int i = 0; i < anchorList.size(); i++) {
                    PointF pointF = anchorList.get(i);
                    if (touchX > (pointF.x - childSideLength / 2) && touchX < (pointF.x + childSideLength / 2) && touchY > (pointF.y - childSideLength / 2) && touchY < (pointF.y + childSideLength / 2)) {
                        onClickIndex(i);
                    }
                }
                break;
        }
        return true;
    }


    private boolean isSelected = false;

    public void setViewPager(ViewPager viewPager) {
        this.viewPager = viewPager;

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (anchorList != null && anchorList.size() > 0 && !isAnimatorStart) {
                    isViewPagerScoll = true;
                    updateDrop(position, positionOffset, positionOffsetPixels);
                }
                // 页面正在滚动时不断调用
                Log.d(TAG, "onPageScrolled————>" + "    position：" + position + "    positionOffest：" + positionOffset + "    positionOffsetPixels：" + positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
//                Log.d(TAG, "onPagerSelected————>    position：" + position);
                isSelected = true;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 0 && isSelected && !isAnimatorStart) {
//                    Log.e(TAG, "onPageScrollStateChanged————>    设置状态：");
                    isSelected = false;
                    isViewPagerScoll = false;
                    bezierCircular.setProgress(direction ? 1.0f : -1.0f);
                    currentPosition = targetPosition;

                    Log.i(TAG, "currentPosition::::" + currentPosition);
                    bezierCircular.resetCircular(anchorList.get(currentPosition));
                    postInvalidate();
                }
                Log.i(TAG, "onPageScrollStateChanged————>    state：" + state);
            }
        });
    }


    //滑动ViewPager时更新指示器的动画
    private void updateDrop(int position, float positionOffset, int positionOffsetPixels) {
        if ((position + positionOffset) - currentPosition > 0) {
            direction = true;
        } else if ((position + positionOffset) - currentPosition < 0) {
            direction = false;
        }

        //防止数组越界
        if ((!direction && currentPosition - 1 < 0) || (direction && currentPosition + 1 > getChildCount() - 1)) {
            return;
        }

        if (direction) targetPosition = currentPosition + 1;
        else targetPosition = currentPosition - 1;

        Log.e(TAG, "direction:::" + direction + "     currentPosition:::" + currentPosition + "     targetPosition:::" + targetPosition);
        bezierCircular.setCurrentAndTarget(anchorList.get(currentPosition), anchorList.get(targetPosition));
        bezierCircular.setProgress(direction ? positionOffset : positionOffset - 1);
        invalidate();
    }


    private void onClickIndex(int position) {
        if (!isAnimatorStart && !isViewPagerScoll) {
            targetPosition = position;
            isAnimatorStart = true;
            startAnimator(); //开始动画
            if (viewPager != null) {
                viewPager.setCurrentItem(position);
            }
//            currentPosition = position;
            Log.i(TAG, "点击了第 " + position + " 项！");
        }
    }

    //绘制子View的背景
    private void drawChildBg(Canvas canvas) {
        if (anchorList == null || anchorList.size() == 0) {
            Log.i(TAG, "锚点位置为空");
            return;
        }

        for (int i = 0; i < anchorList.size(); i++) {
            PointF pointF = anchorList.get(i);
            canvas.drawCircle(pointF.x, pointF.y, (childSideLength - 4) / 2, childBgPaint);
        }
    }

    /**
     * 切换动画
     */
    private void startAnimator() {
        bezierCircular.setCurrentAndTarget(anchorList.get(currentPosition), anchorList.get(targetPosition));
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, targetPosition > currentPosition ? 1 : -1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bezierCircular.setProgress((Float) animation.getAnimatedValue());
                postInvalidate();
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentPosition = targetPosition;
                bezierCircular.resetCircular(anchorList.get(currentPosition));
                isAnimatorStart = false;
                postInvalidate();
                super.onAnimationEnd(animation);
            }
        });

        int count = Math.abs(targetPosition - currentPosition);
        if (count == 0) {
            return;
        }
        int duration = 600;
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }
}
