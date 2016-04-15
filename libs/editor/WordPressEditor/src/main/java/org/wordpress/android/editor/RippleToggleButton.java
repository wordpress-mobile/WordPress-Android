package org.wordpress.android.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ToggleButton;

public class RippleToggleButton extends ToggleButton {
    private static final int FRAME_RATE = 10;
    private static final int DURATION = 250;
    private static final int FILL_INITIAL_OPACITY = 200;
    private static final int STROKE_INITIAL_OPACITY = 255;

    private float mHalfWidth;
    private boolean mAnimationIsRunning = false;
    private int mTimer = 0;
    private Paint mFillPaint;
    private Paint mStrokePaint;

    public RippleToggleButton(Context context) {
        this(context, null);
   }

    public RippleToggleButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        int rippleColor = getResources().getColor(R.color.format_bar_ripple_animation);

        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(rippleColor);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setAlpha(FILL_INITIAL_OPACITY);

        mStrokePaint = new Paint();
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setColor(rippleColor);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(2);
        mStrokePaint.setAlpha(STROKE_INITIAL_OPACITY);

        setWillNotDraw(false);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        if (mAnimationIsRunning) {
            if (DURATION <= mTimer * FRAME_RATE) {
                mAnimationIsRunning = false;
                mTimer = 0;
            } else {
                float progressFraction = ((float) mTimer * FRAME_RATE) / DURATION;

                mFillPaint.setAlpha((int) (FILL_INITIAL_OPACITY * (1 - progressFraction)));
                mStrokePaint.setAlpha((int) (STROKE_INITIAL_OPACITY * (1 - progressFraction)));

                canvas.drawCircle(mHalfWidth, mHalfWidth, mHalfWidth * progressFraction, mFillPaint);
                canvas.drawCircle(mHalfWidth, mHalfWidth, mHalfWidth * progressFraction, mStrokePaint);

                mTimer++;
            }

            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        startRippleAnimation();
        return super.onTouchEvent(event);
    }

    private void startRippleAnimation() {
        if (this.isEnabled() && !mAnimationIsRunning) {
            mHalfWidth = getMeasuredWidth() / 2;
            mAnimationIsRunning = true;
            invalidate();
        }
    }
}
