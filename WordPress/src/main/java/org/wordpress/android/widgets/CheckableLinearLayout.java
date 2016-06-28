package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.wordpress.android.R;

/**
 * Created by Will on 6/26/16.
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private Boolean mIsChecked;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setChecked(boolean b) {
        RadioButton radioButton = (RadioButton) findViewById(R.id.radio_button);
        if (radioButton != null) {
            radioButton.setChecked(b);
        }
        mIsChecked = b;
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }
}
