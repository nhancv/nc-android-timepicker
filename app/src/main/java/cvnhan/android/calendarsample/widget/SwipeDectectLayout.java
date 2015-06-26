package cvnhan.android.calendarsample.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by cvnhan on 26-Jun-15.
 */
public class SwipeDectectLayout extends ViewGroup {

    private static final String LOG_TAG = SwipeDectectLayout.class.getSimpleName();
    private View mTarget;
    private SwipeDectectLayout.OnRefreshListener mListener;
    private boolean mRefreshing;
    private int mTouchSlop;
    private float mTotalDragDistance;
    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    private boolean mOriginalOffsetCalculated;
    private float mInitialMotionX;
    private float mInitialDownX;
    private boolean mIsBeingDragged;
    private int mActivePointerId;
    private boolean mScale;
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{16842766};
    private CircleImageView mCircleView;
    private int mCircleViewIndex;
    protected int mFrom;
    private float mStartingScale;
    protected int mOriginalOffsetTop;
    private Animation mScaleAnimation;
    private Animation mScaleDownAnimation;
    private Animation mScaleDownToStartAnimation;
    private float mSpinnerFinalOffset;
    private boolean mNotify;
    private int mCircleWidth;
    private int mCircleHeight;
    private boolean mUsingCustomStart;
    private Animation.AnimationListener mRefreshListener;
    private int directionSwipe = 0; //0 left-right, 1 right-left;
    private final Animation mAnimateToCorrectPosition;
    private final Animation mAnimateToStartPosition;

    private LinearLayout.LayoutParams mCompressedParams = null;
    private int originalWidth = 0;

    private void setColorViewAlpha(int targetAlpha) {
        this.mCircleView.getBackground().setAlpha(targetAlpha);
    }

    public SwipeDectectLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public SwipeDectectLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRefreshing = false;
        this.mTotalDragDistance = -1.0F;
        this.mOriginalOffsetCalculated = false;
        this.mActivePointerId = -1;
        this.mCircleViewIndex = -1;
        this.mRefreshListener = new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                if (SwipeDectectLayout.this.mRefreshing) {
                    if (SwipeDectectLayout.this.mNotify && SwipeDectectLayout.this.mListener != null) {
                        if (directionSwipe == 0)
                            SwipeDectectLayout.this.mListener.onLefttoRight();
                        else
                            SwipeDectectLayout.this.mListener.onRighttoLeft();
                    }
                } else {
                    SwipeDectectLayout.this.mCircleView.setVisibility(View.GONE);
                    SwipeDectectLayout.this.setColorViewAlpha(255);
                    if (SwipeDectectLayout.this.mScale) {
                        SwipeDectectLayout.this.setAnimationProgress(0.0F);
                    } else {
                        SwipeDectectLayout.this.setTargetOffsetTopAndBottom(SwipeDectectLayout.this.mOriginalOffsetTop - SwipeDectectLayout.this.mCurrentTargetOffsetTop, true);
                    }
                }

                SwipeDectectLayout.this.mCurrentTargetOffsetTop = SwipeDectectLayout.this.mCircleView.getTop();
            }
        };

        this.mAnimateToCorrectPosition = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                boolean targetTop = false;
                boolean endTarget = false;
                int endTarget1;
                if (!SwipeDectectLayout.this.mUsingCustomStart) {
                    endTarget1 = (int) (SwipeDectectLayout.this.mSpinnerFinalOffset - (float) Math.abs(SwipeDectectLayout.this.mOriginalOffsetTop));
                } else {
                    endTarget1 = (int) SwipeDectectLayout.this.mSpinnerFinalOffset;
                }

                int targetTop1 = SwipeDectectLayout.this.mFrom + (int) ((float) (endTarget1 - SwipeDectectLayout.this.mFrom) * interpolatedTime);
                int offset = targetTop1 - SwipeDectectLayout.this.mCircleView.getTop();
                SwipeDectectLayout.this.setTargetOffsetTopAndBottom(offset, false);
            }
        };
        this.mAnimateToStartPosition = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                SwipeDectectLayout.this.moveToStart(interpolatedTime);
            }
        };
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mMediumAnimationDuration = 2;
        this.setWillNotDraw(false);
        this.mDecelerateInterpolator = new DecelerateInterpolator(2.0F);
        TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        this.setEnabled(a.getBoolean(0, true));
        a.recycle();
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        this.mCircleWidth = (int) (40.0F * metrics.density);
        this.mCircleHeight = (int) (40.0F * metrics.density);
        this.createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        this.mSpinnerFinalOffset = 64.0F * metrics.density;
        this.mTotalDragDistance = this.mSpinnerFinalOffset;
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        return this.mCircleViewIndex < 0 ? i : (i == childCount - 1 ? this.mCircleViewIndex : (i >= this.mCircleViewIndex ? i + 1 : i));
    }

    private void createProgressView() {
        this.mCircleView = new CircleImageView(this.getContext(), -328966, 20.0F);
        this.mCircleView.setVisibility(View.VISIBLE);
        this.addView(this.mCircleView);
    }

    public void setOnRefreshListener(SwipeDectectLayout.OnRefreshListener listener) {
        this.mListener = listener;
    }

    private boolean isAlphaUsedForScale() {
        return Build.VERSION.SDK_INT < 11;
    }

    public void setRefreshing(boolean refreshing) {
        if (refreshing && this.mRefreshing != refreshing) {
            this.mRefreshing = refreshing;
            int endTarget1;
            if (!this.mUsingCustomStart) {
                endTarget1 = (int) (this.mSpinnerFinalOffset + (float) this.mOriginalOffsetTop);
            } else {
                endTarget1 = (int) this.mSpinnerFinalOffset;
            }

            this.setTargetOffsetTopAndBottom(endTarget1 - this.mCurrentTargetOffsetTop, true);
            this.mNotify = false;
            this.startScaleUpAnimation(this.mRefreshListener);
        } else {
            this.setRefreshing(refreshing, false);
        }

    }

    private void startScaleUpAnimation(Animation.AnimationListener listener) {
        this.mCircleView.setVisibility(View.VISIBLE);

        this.mScaleAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                SwipeDectectLayout.this.setAnimationProgress(interpolatedTime);
            }
        };
        this.mScaleAnimation.setDuration((long) this.mMediumAnimationDuration);
        if (listener != null) {
            this.mCircleView.setAnimationListener(listener);
        }

        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mScaleAnimation);
    }

    private void setAnimationProgress(float progress) {
        if (this.isAlphaUsedForScale()) {
            this.setColorViewAlpha((int) (progress * 255.0F));
        } else {
            ViewCompat.setScaleX(this.mCircleView, progress);
            ViewCompat.setScaleY(this.mCircleView, progress);
        }

    }

    private void setRefreshing(boolean refreshing, boolean notify) {
        if (this.mRefreshing != refreshing) {
            this.mNotify = notify;
            this.ensureTarget();
            this.mRefreshing = refreshing;
            if (this.mRefreshing) {
                this.animateOffsetToCorrectPosition(this.mCurrentTargetOffsetTop, this.mRefreshListener);
            } else {
                this.startScaleDownAnimation(this.mRefreshListener);
            }
        }

    }

    private void startScaleDownAnimation(Animation.AnimationListener listener) {
        this.mScaleDownAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                SwipeDectectLayout.this.setAnimationProgress(1.0F - interpolatedTime);
            }
        };
        this.mScaleDownAnimation.setDuration(150L);
        this.mCircleView.setAnimationListener(listener);
        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mScaleDownAnimation);
    }

    public boolean isRefreshing() {
        return this.mRefreshing;
    }

    private void ensureTarget() {
        if (this.mTarget == null) {
            for (int i = 0; i < this.getChildCount(); ++i) {
                View child = this.getChildAt(i);
                if (!child.equals(this.mCircleView)) {
                    this.mTarget = child;
                    break;
                }
            }
        }

    }

    public void setDistanceToTriggerSync(int distance) {
        this.mTotalDragDistance = (float) distance;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = this.getMeasuredWidth();
        int height = this.getMeasuredHeight();
        if (this.getChildCount() != 0) {
            if (this.mTarget == null) {
                this.ensureTarget();
            }

            if (this.mTarget != null) {
                View child = this.mTarget;
                int childLeft = this.getPaddingLeft();
                int childTop = this.getPaddingTop();
                int childWidth = width - this.getPaddingLeft() - this.getPaddingRight();
                int childHeight = height - this.getPaddingTop() - this.getPaddingBottom();
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                int circleWidth = this.mCircleView.getMeasuredWidth();
                int circleHeight = this.mCircleView.getMeasuredHeight();
                this.mCircleView.layout(width / 2 - circleWidth / 2, this.mCurrentTargetOffsetTop, width / 2 + circleWidth / 2, this.mCurrentTargetOffsetTop + circleHeight);
            }
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (this.mTarget == null) {
            this.ensureTarget();
        }

        if (this.mTarget != null) {
            this.mTarget.measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth() - this.getPaddingLeft() - this.getPaddingRight(), 1073741824), MeasureSpec.makeMeasureSpec(this.getMeasuredHeight() - this.getPaddingTop() - this.getPaddingBottom(), 1073741824));
            this.mCircleView.measure(MeasureSpec.makeMeasureSpec(this.mCircleWidth, 1073741824), MeasureSpec.makeMeasureSpec(this.mCircleHeight, 1073741824));
            if (!this.mUsingCustomStart && !this.mOriginalOffsetCalculated) {
                this.mOriginalOffsetCalculated = true;
                this.mCurrentTargetOffsetTop = this.mOriginalOffsetTop = -this.mCircleView.getMeasuredHeight();
            }

            this.mCircleViewIndex = -1;

            for (int index = 0; index < this.getChildCount(); ++index) {
                if (this.getChildAt(index) == this.mCircleView) {
                    this.mCircleViewIndex = index;
                    break;
                }
            }

        }
    }

    public int getProgressCircleDiameter() {
        return this.mCircleView != null ? this.mCircleView.getMeasuredHeight() : 0;
    }

    public boolean canChildScrollUp() {
        if (Build.VERSION.SDK_INT >= 14) {
            return ViewCompat.canScrollVertically(this.mTarget, -1);
        } else if (this.mTarget instanceof AbsListView) {
            AbsListView absListView = (AbsListView) this.mTarget;
            return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
        } else {
            return ViewCompat.canScrollVertically(this.mTarget, -1) || this.mTarget.getScrollY() > 0;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.ensureTarget();
        int action = MotionEventCompat.getActionMasked(ev);
        if (this.mReturningToStart && action == 0) {
            this.mReturningToStart = false;
        }

//        if(this.isEnabled() && !this.mReturningToStart && !this.canChildScrollUp() && !this.mRefreshing) {
        if (this.isEnabled() && !this.mReturningToStart && !this.mRefreshing) {
            switch (action) {
                case 0:
                    this.setTargetOffsetTopAndBottom(this.mOriginalOffsetTop - this.mCircleView.getTop(), true);
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                    this.mIsBeingDragged = false;
                    float initialDownX = this.getMotionEventX(ev, this.mActivePointerId);
                    if (initialDownX == -1.0F) {
                        return false;
                    }
                    this.mInitialDownX = initialDownX;

                    break;
                case 1:
                case 3:
                    this.mIsBeingDragged = false;
                    this.mActivePointerId = -1;
                    break;
                case 2:
                    if (this.mActivePointerId == -1) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but don\'t have an active pointer id.");
                        return false;
                    }

                    float x = this.getMotionEventX(ev, this.mActivePointerId);
                    if (x == -1.0F) {
                        return false;
                    }

                    if (originalWidth == 0) {
                        originalWidth = getWidth();
                        mCompressedParams = new LinearLayout.LayoutParams(
                                originalWidth, LinearLayout.LayoutParams.MATCH_PARENT);
                        setLayoutParams(mCompressedParams);
                    }
                    float xDiff = x - this.mInitialDownX;
                    if (Math.abs(xDiff) > (float) this.mTouchSlop && !this.mIsBeingDragged) {
                        if (xDiff > 0) {
                            this.mInitialMotionX = this.mInitialDownX + (float) this.mTouchSlop;
                            this.directionSwipe = 0;
                        } else {
                            this.mInitialMotionX = (this.mInitialDownX + (float) this.mTouchSlop) * -1;
                            this.directionSwipe = 1;
                        }
                        this.mIsBeingDragged = true;
                    }
                case 4:
                case 5:
                default:
                    break;
                case 6:
                    this.onSecondaryPointerUp(ev);
            }

            return this.mIsBeingDragged;
        } else {
            return false;
        }
    }


    private float getMotionEventX(MotionEvent ev, int activePointerId) {
        int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        return index < 0 ? -1.0F : MotionEventCompat.getX(ev, index);
    }

    public void requestDisallowInterceptTouchEvent(boolean b) {
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        if (this.mReturningToStart && action == 0) {
            this.mReturningToStart = false;
        }
//        if(this.isEnabled() && !this.mReturningToStart && !this.canChildScrollUp()) {
        if (this.isEnabled() && !this.mReturningToStart) {
            int pointerIndex;
            float x;
            float overscrollTop;
            switch (action) {
                case 0:
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                    this.mIsBeingDragged = false;
                    break;
                case 1:
                case 3:
                    if (this.mActivePointerId == -1) {
                        if (action == 1) {
                            Log.e(LOG_TAG, "Got ACTION_UP event but don\'t have an active pointer id.");
                        }

                        return false;
                    }

                    pointerIndex = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                    x = MotionEventCompat.getX(ev, pointerIndex);
                    overscrollTop = (x - this.mInitialMotionX) * 0.5F;
                    this.mIsBeingDragged = false;
                    if (overscrollTop > this.mTotalDragDistance) {
                        this.setRefreshing(true, true);
                    } else {
                        this.mRefreshing = false;
                        Animation.AnimationListener listener1 = null;
                        if (!this.mScale) {
                            listener1 = new Animation.AnimationListener() {
                                public void onAnimationStart(Animation animation) {
                                }

                                public void onAnimationEnd(Animation animation) {
                                    if (!SwipeDectectLayout.this.mScale) {
                                        SwipeDectectLayout.this.startScaleDownAnimation((Animation.AnimationListener) null);
                                    }

                                }

                                public void onAnimationRepeat(Animation animation) {
                                }
                            };
                        }

                        this.animateOffsetToStartPosition(this.mCurrentTargetOffsetTop, listener1);
                    }

                    this.mActivePointerId = -1;
                    return false;
                case 2:
                    pointerIndex = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                    if (pointerIndex < 0) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                        return false;
                    }

                    x = MotionEventCompat.getX(ev, pointerIndex);
                    overscrollTop = (x - this.mInitialMotionX) * 0.5F;
                    if (this.mIsBeingDragged) {
                        float listener = overscrollTop / this.mTotalDragDistance;
                        if (listener < 0.0F) {
                            return false;
                        }

                        float dragPercent = Math.min(1.0F, Math.abs(listener));
                        float adjustedPercent = (float) Math.max((double) dragPercent - 0.4D, 0.0D) * 5.0F / 3.0F;
                        float extraOS = Math.abs(overscrollTop) - this.mTotalDragDistance;
                        float slingshotDist = this.mUsingCustomStart ? this.mSpinnerFinalOffset - (float) this.mOriginalOffsetTop : this.mSpinnerFinalOffset;
                        float tensionSlingshotPercent = Math.max(0.0F, Math.min(extraOS, slingshotDist * 2.0F) / slingshotDist);
                        float tensionPercent = (float) ((double) (tensionSlingshotPercent / 4.0F) - Math.pow((double) (tensionSlingshotPercent / 4.0F), 2.0D)) * 2.0F;
                        float extraMove = slingshotDist * tensionPercent * 2.0F;
                        int targetX = this.mOriginalOffsetTop + (int) (slingshotDist * dragPercent + extraMove);
                        if (this.mCircleView.getVisibility() != View.VISIBLE) {
                            this.mCircleView.setVisibility(View.VISIBLE);
                        }

                        if (!this.mScale) {
                            ViewCompat.setScaleX(this.mCircleView, 1.0F);
                            ViewCompat.setScaleY(this.mCircleView, 1.0F);
                        }

                        float rotation;
                        if (overscrollTop < this.mTotalDragDistance) {
                            if (this.mScale) {
                                this.setAnimationProgress(overscrollTop / this.mTotalDragDistance);
                            }

                            rotation = adjustedPercent * 0.8F;
                        }

                        rotation = (-0.25F + 0.4F * adjustedPercent + tensionPercent * 2.0F) * 0.5F;
                        this.setTargetOffsetTopAndBottom(targetX - this.mCurrentTargetOffsetTop, true);
                    }
                case 4:
                default:
                    break;
                case 5:
                    pointerIndex = MotionEventCompat.getActionIndex(ev);
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                    break;
                case 6:
                    this.onSecondaryPointerUp(ev);
            }

            return true;
        } else {
            return false;
        }
    }

    public void restoreOriginalLayout() {
        if (originalWidth == 0) originalWidth = 100;
        if (mCompressedParams == null)
            mCompressedParams = new LinearLayout.LayoutParams(
                    originalWidth, LinearLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(mCompressedParams);
        requestLayout();
    }

    private void animateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
        this.mFrom = from;
        this.mAnimateToCorrectPosition.reset();
        this.mAnimateToCorrectPosition.setDuration(200L);
        this.mAnimateToCorrectPosition.setInterpolator(this.mDecelerateInterpolator);
        if (listener != null) {
            this.mCircleView.setAnimationListener(listener);
        }

        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        if (this.mScale) {
            this.startScaleDownReturnToStartAnimation(from, listener);
        } else {
            this.mFrom = from;
            this.mAnimateToStartPosition.reset();
            this.mAnimateToStartPosition.setDuration(200L);
            this.mAnimateToStartPosition.setInterpolator(this.mDecelerateInterpolator);
            if (listener != null) {
                this.mCircleView.setAnimationListener(listener);
            }

            this.mCircleView.clearAnimation();
            this.mCircleView.startAnimation(this.mAnimateToStartPosition);
        }

    }

    private void moveToStart(float interpolatedTime) {
        boolean targetTop = false;
        int targetTop1 = this.mFrom + (int) ((float) (this.mOriginalOffsetTop - this.mFrom) * interpolatedTime);
        int offset = targetTop1 - this.mCircleView.getTop();
        this.setTargetOffsetTopAndBottom(offset, false);
    }

    private void startScaleDownReturnToStartAnimation(int from, Animation.AnimationListener listener) {
        this.mFrom = from;
        if (this.isAlphaUsedForScale()) {
        } else {
            this.mStartingScale = ViewCompat.getScaleX(this.mCircleView);
        }

        this.mScaleDownToStartAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = SwipeDectectLayout.this.mStartingScale + -SwipeDectectLayout.this.mStartingScale * interpolatedTime;
                SwipeDectectLayout.this.setAnimationProgress(targetScale);
                SwipeDectectLayout.this.moveToStart(interpolatedTime);
            }
        };
        this.mScaleDownToStartAnimation.setDuration(50L);
        if (listener != null) {
            this.mCircleView.setAnimationListener(listener);
        }

        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {

        this.mCircleView.bringToFront();
        this.mCircleView.offsetLeftAndRight(offset);
        this.mCurrentTargetOffsetTop = this.mCircleView.getTop();
        if (requiresUpdate && Build.VERSION.SDK_INT < 11) {
            this.invalidate();
        }

    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = MotionEventCompat.getActionIndex(ev);
        int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }

    }

    public interface OnRefreshListener {
        void onLefttoRight();

        void onRighttoLeft();
    }
}

class CircleImageView extends ImageView {
    private Animation.AnimationListener mListener;
    private int mShadowRadius;

    public CircleImageView(Context context, int color, float radius) {
        super(context);
        float density = this.getContext().getResources().getDisplayMetrics().density;
        int diameter = (int) (radius * density * 2.0F);
        int shadowYOffset = (int) (density * 1.75F);
        int shadowXOffset = (int) (density * 0.0F);
        this.mShadowRadius = (int) (density * 3.5F);
        ShapeDrawable circle;
        if (this.elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, 4.0F * density);
        } else {
            CircleImageView.OvalShadow oval = new CircleImageView.OvalShadow(this.mShadowRadius, diameter);
            circle = new ShapeDrawable(oval);
            ViewCompat.setLayerType(this, 1, circle.getPaint());
            circle.getPaint().setShadowLayer((float) this.mShadowRadius, (float) shadowXOffset, (float) shadowYOffset, 503316480);
            int padding = this.mShadowRadius;
            this.setPadding(padding, padding, padding, padding);
        }

        circle.getPaint().setColor(color);
        this.setBackgroundDrawable(circle);
    }

    private boolean elevationSupported() {
        return Build.VERSION.SDK_INT >= 21;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!this.elevationSupported()) {
            this.setMeasuredDimension(this.getMeasuredWidth() + this.mShadowRadius * 2, this.getMeasuredHeight() + this.mShadowRadius * 2);
        }

    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        this.mListener = listener;
    }

    public void onAnimationStart() {
        super.onAnimationStart();
        if (this.mListener != null) {
            this.mListener.onAnimationStart(this.getAnimation());
        }

    }

    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (this.mListener != null) {
            this.mListener.onAnimationEnd(this.getAnimation());
        }

    }

    public void setBackgroundColorRes(int colorRes) {
        this.setBackgroundColor(this.getContext().getResources().getColor(colorRes));
    }

    public void setBackgroundColor(int color) {
        if (this.getBackground() instanceof ShapeDrawable) {
            ((ShapeDrawable) this.getBackground()).getPaint().setColor(color);
        }

    }

    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private Paint mShadowPaint = new Paint();
        private int mCircleDiameter;

        public OvalShadow(int shadowRadius, int circleDiameter) {
            CircleImageView.this.mShadowRadius = shadowRadius;
            this.mCircleDiameter = circleDiameter;
            this.mRadialGradient = new RadialGradient((float) (this.mCircleDiameter / 2), (float) (this.mCircleDiameter / 2), (float) CircleImageView.this.mShadowRadius, new int[]{1023410176, 0}, (float[]) null, Shader.TileMode.CLAMP);
            this.mShadowPaint.setShader(this.mRadialGradient);
        }

        public void draw(Canvas canvas, Paint paint) {
            int viewWidth = CircleImageView.this.getWidth();
            int viewHeight = CircleImageView.this.getHeight();
            canvas.drawCircle((float) (viewWidth / 2), (float) (viewHeight / 2), (float) (this.mCircleDiameter / 2 + CircleImageView.this.mShadowRadius), this.mShadowPaint);
            canvas.drawCircle((float) (viewWidth / 2), (float) (viewHeight / 2), (float) (this.mCircleDiameter / 2), paint);
        }
    }
}
