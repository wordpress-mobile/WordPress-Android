package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.wordpress.android.R;

public class WPTintImageView extends ImageView {

    private ColorStateList mTint;

    public WPTintImageView(Context context) {
        super(context);
    }

    public WPTintImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WPTintImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WPTintImageView, defStyle, 0);
        mTint = a.getColorStateList(R.styleable.WPTintImageView_tint);
        a.recycle();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mTint != null && mTint.isStateful())
            updateTintColor();
    }

    public void setColorFilter(ColorStateList tint) {
        this.mTint = tint;
        super.setColorFilter(tint.getColorForState(getDrawableState(), 0));
    }

    private void updateTintColor() {
        int color = mTint.getColorForState(getDrawableState(), 0);
        setColorFilter(color);
    }

}