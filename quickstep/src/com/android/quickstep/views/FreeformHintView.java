/*
 * Copyright (C) 2026 The AviumUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.FloatProperty;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.android.app.animation.Interpolators;
import com.android.launcher3.R;
import com.android.launcher3.views.BaseDragLayer;

public class FreeformHintView extends View {

    public static final FloatProperty<FreeformHintView> HINT_ALPHA =
            new FloatProperty<FreeformHintView>("hintAlpha") {
                @Override
                public void setValue(FreeformHintView view, float alpha) {
                    view.setHintAlpha(alpha);
                }
                @Override
                public Float get(FreeformHintView view) {
                    return view.getHintAlpha();
                }
            };

    private static final int CARD_HEIGHT_DP = 56;
    private static final int CORNER_RADIUS_DP = 28;
    private static final int ICON_SIZE_DP = 24;
    private static final int ICON_PADDING_DP = 16;
    private static final int TEXT_SIZE_SP = 14;
    private static final int CARD_MARGIN_DP = 12;

    private float mHintAlpha = 0f;
    private final Paint mBgPaint;
    private final Paint mTextPaint;
    private final Paint mIconPaint;
    private final RectF mCardRect = new RectF();
    private final String mHintText;
    private final String mSwipeUpHintText;
    private boolean mShowSwipeUpText = true;
    private float mDensity;
    private float mCardHeight;
    private float mCornerRadius;
    private float mIconSize;
    private float mIconPadding;
    private int mCardMargin;
    private int mAnimatedWidth = -1;

    // TaskView attachment for release phase expansion
    private int mTaskLeft, mTaskTop, mTaskWidth, mTaskHeight;
    private boolean mHasTaskBounds = false;
    private float mReleaseProgress = 0f; // 0..1 during release phase

    public FreeformHintView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        mDensity = context.getResources().getDisplayMetrics().density;
        mCardHeight = CARD_HEIGHT_DP * mDensity;
        mCornerRadius = CORNER_RADIUS_DP * mDensity;
        mIconSize = ICON_SIZE_DP * mDensity;
        mIconPadding = ICON_PADDING_DP * mDensity;
        mCardMargin = (int) (CARD_MARGIN_DP * mDensity);

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(TEXT_SIZE_SP * mDensity);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        mIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mIconPaint.setColor(Color.WHITE);
        mIconPaint.setStyle(Paint.Style.STROKE);
        mIconPaint.setStrokeWidth(2 * mDensity);
        mIconPaint.setStrokeJoin(Paint.Join.ROUND);
        mIconPaint.setStrokeCap(Paint.Cap.ROUND);

        mHintText = context.getString(R.string.avium_gesture_freeform_hint);
        mSwipeUpHintText = context.getString(R.string.avium_gesture_swipe_up_hint);
    }

    private int computeCardWidth() {
        String displayText = mShowSwipeUpText ? mSwipeUpHintText : mHintText;
        float textWidth = mTextPaint.measureText(displayText);
        float neededWidth = mIconPadding + mIconSize + 12 * mDensity + textWidth + mIconPadding;
        return (int) (neededWidth + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w;
        int h;

        if (mReleaseProgress > 0f && mHasTaskBounds) {
            // Interpolate from small card size to wrapping TaskView
            int smallW = computeCardWidth();
            int smallH = (int) mCardHeight;
            int bigW = mTaskWidth + mCardMargin * 2;
            int bigH = mTaskHeight + mCardMargin * 2;

            w = (int) (smallW + (bigW - smallW) * mReleaseProgress);
            h = (int) (smallH + (bigH - smallH) * mReleaseProgress);
        } else {
            w = mAnimatedWidth >= 0 ? mAnimatedWidth : computeCardWidth();
            h = (int) mCardHeight;
        }

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    private void updatePosition() {
        if (!(getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) getParent();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) return;

        int parentW = parent.getWidth();
        int smallCardW = computeCardWidth();
        int curCardW = getMeasuredWidth();
        if (curCardW <= 0) curCardW = smallCardW;
        int smallX = parentW - smallCardW - mCardMargin;
        int smallY = mCardMargin + getStatusBarHeight();

        int l, t;
        if (mReleaseProgress > 0f && mHasTaskBounds) {
            // Interpolate center position to keep card centered on target
            int smallCenterX = parentW - mCardMargin - smallCardW / 2;
            int bigCenterX = mTaskLeft + mTaskWidth / 2;
            int centerX = (int) (smallCenterX + (bigCenterX - smallCenterX) * mReleaseProgress);
            int centerY = (int) (smallY + ((mTaskTop + mTaskHeight / 2) - smallY) * mReleaseProgress);
            l = centerX - curCardW / 2;
            t = centerY - getMeasuredHeight() / 2;
        } else {
            l = smallX;
            t = smallY;
        }

        // Clamp
        l = Math.max(0, Math.min(l, parentW - curCardW));
        t = Math.max(0, t);

        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = l;
        lp.topMargin = t;
        lp.rightMargin = 0;
        lp.bottomMargin = 0;
        requestLayout();
    }

    public void setReleaseProgress(float progress) {
        progress = Math.max(0, Math.min(1, progress));
        if (mReleaseProgress == progress) return;
        mReleaseProgress = progress;
        updatePosition();
        requestLayout();
        invalidate();
    }

    public void setTaskBounds(int taskLeft, int taskTop, int taskWidth, int taskHeight) {
        mTaskLeft = taskLeft;
        mTaskTop = taskTop;
        mTaskWidth = taskWidth;
        mTaskHeight = taskHeight;
        mHasTaskBounds = true;

        if (mReleaseProgress > 0f) {
            updatePosition();
            requestLayout();
        }
    }

    public void clearTaskBounds() {
        mHasTaskBounds = false;
        mReleaseProgress = 0f;
        updatePosition();
        requestLayout();
    }

    public void setHintAlpha(float alpha) {
        alpha = Math.max(0, Math.min(1, alpha));
        if (mHintAlpha == alpha) return;
        mHintAlpha = alpha;

        int bgAlpha = (int) (200 * alpha);
        mBgPaint.setColor(Color.argb(bgAlpha, 30, 30, 30));
        mTextPaint.setAlpha((int) (255 * alpha));
        mIconPaint.setAlpha((int) (255 * alpha));

        if (alpha > 0.01f && getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }

        float scale = 0.85f + 0.15f * alpha;
        setScaleX(scale);
        setScaleY(scale);
        setAlpha(alpha);
        invalidate();
    }

    public float getHintAlpha() {
        return mHintAlpha;
    }

    public void setShowSwipeUpText(boolean show) {
        if (mShowSwipeUpText == show) return;
        mShowSwipeUpText = show;

        if (!show && !mHasTaskBounds) {
            // Entering release but no task bounds yet
            updatePosition();
            requestLayout();
        } else if (show) {
            // Switching back to swipe-up
            mReleaseProgress = 0f;
            updatePosition();
            requestLayout();
        }

        int newWidth = computeCardWidth();
        int oldWidth = getWidth();
        if (oldWidth > 0 && newWidth != oldWidth && show) {
            animate().cancel();
            ValueAnimator anim = ValueAnimator.ofInt(oldWidth, newWidth);
            anim.setDuration(200L);
            anim.setInterpolator(Interpolators.EMPHASIZED);
            anim.addUpdateListener(a -> {
                mAnimatedWidth = (int) a.getAnimatedValue();
                requestLayout();
            });
            anim.start();
        }

        invalidate();
    }

    public void animateOut(Runnable onEnd) {
        animate().cancel();
        animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200L)
                .setInterpolator(Interpolators.EMPHASIZED)
                .withEndAction(() -> {
                    setVisibility(View.GONE);
                    mHintAlpha = 0f;
                    setHintAlpha(0f);
                    mReleaseProgress = 0f;
                    mHasTaskBounds = false;
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mHintAlpha <= 0.01f) return;

        int width = getWidth();
        int height = getHeight();

        // Card fills the entire view
        mCardRect.set(0, 0, width, height);

        canvas.drawRoundRect(mCardRect, mCornerRadius, mCornerRadius, mBgPaint);

        // Only draw icon and text when card is small (swipe-up phase)
        if (mReleaseProgress < 0.5f) {
            float iconAlpha = 1f - mReleaseProgress * 2f;
            float textAlpha = 1f - mReleaseProgress * 2f;

            if (iconAlpha > 0) {
                float iconLeft = mIconPadding;
                float iconTop = (height - mIconSize) / 2f;

                mIconPaint.setStyle(Paint.Style.STROKE);
                mIconPaint.setAlpha((int) (255 * mHintAlpha * iconAlpha));
                canvas.drawRoundRect(
                        iconLeft, iconTop,
                        iconLeft + mIconSize, iconTop + mIconSize,
                        3 * mDensity, 3 * mDensity,
                        mIconPaint);

                float smallInset = 4 * mDensity;
                mIconPaint.setStyle(Paint.Style.FILL);
                mIconPaint.setAlpha((int) (180 * mHintAlpha * iconAlpha));
                canvas.drawRoundRect(
                        iconLeft + smallInset, iconTop + smallInset,
                        iconLeft + mIconSize - smallInset, iconTop + mIconSize - smallInset,
                        2 * mDensity, 2 * mDensity,
                        mIconPaint);
            }

            if (textAlpha > 0) {
                mTextPaint.setAlpha((int) (255 * mHintAlpha * textAlpha));
                String displayText = mShowSwipeUpText ? mSwipeUpHintText : mHintText;
                float textX = mIconPadding + mIconSize + 12 * mDensity;
                float textY = height / 2f - (mTextPaint.descent() + mTextPaint.ascent()) / 2f;
                canvas.drawText(displayText, textX, textY, mTextPaint);
            }
        }
    }

    public void attachToContainer(RecentsViewContainer container) {
        ViewGroup dragLayer = container.getDragLayer();
        if (dragLayer == null || getParent() != null) return;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = dragLayer.getWidth() - computeCardWidth() - mCardMargin;
        lp.topMargin = mCardMargin + getStatusBarHeight();

        setVisibility(View.GONE);
        dragLayer.addView(this, lp);
        // Refresh position after layout
        post(this::updatePosition);
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return getResources().getDimensionPixelSize(resId);
        }
        return (int) (24 * mDensity);
    }

    private int getNavBarHeight() {
        int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resId > 0) {
            return getResources().getDimensionPixelSize(resId);
        }
        return (int) (48 * mDensity);
    }

    public void detachFromContainer() {
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    public void destroy() {
        detachFromContainer();
    }
}
