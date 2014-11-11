package org.wordpress.android.ui.media;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class CheckableFrameLayout extends FrameLayout implements Checkable {
    private boolean mIsChecked;

    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mIsChecked = false;
    }

    @Override
    public void setChecked(boolean checked) {
        mIsChecked = checked;
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void toggle() {
        mIsChecked = !mIsChecked;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        if (isChecked()) {
            mergeDrawableStates(drawableState, new int[] { android.R.attr.state_checked });
        }

        return drawableState;
    }
}
