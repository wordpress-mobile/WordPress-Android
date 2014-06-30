package org.wordpress.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

public class CheckedLinearLayout extends LinearLayout implements Checkable {
    private CheckedTextView mCheckbox;

    public CheckedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View v = getChildAt(i);
            if (v instanceof CheckedTextView) {
                mCheckbox = (CheckedTextView)v;
            }
        }
    }

    @Override
    public boolean isChecked() {
        return mCheckbox != null ? mCheckbox.isChecked() : false;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mCheckbox != null) {
            mCheckbox.setChecked(checked);
        }
    }

    @Override
    public void toggle() {
        if (mCheckbox != null) {
            mCheckbox.toggle();
        }
    }
}