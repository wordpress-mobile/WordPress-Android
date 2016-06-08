package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

import org.wordpress.android.R;

/**
 * {@link LinearLayout} subclass that provides {@link Checkable} functionality.
 *
 * This layout will update an child view with id=R.id.checkable if one is available. The checked
 * state of this layout is tracked independent of the child view existing.
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private boolean mChecked;
    private Checkable mCheckable;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCheckable = (Checkable) findViewById(R.id.checkable);
        refreshCheckableChild();
    }

    @Override
    public void setChecked(boolean checked) {
        mChecked = checked;
        refreshCheckableChild();
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    private void refreshCheckableChild() {
        if (mCheckable != null && mCheckable.isChecked() != mChecked) {
            mCheckable.setChecked(mChecked);
        }
    }
}
