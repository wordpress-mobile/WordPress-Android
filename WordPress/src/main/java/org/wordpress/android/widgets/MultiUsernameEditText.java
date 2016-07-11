package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;


/**
 *  Used to handle backspace in People Management username field
 *  http://stackoverflow.com/a/24624767/569430/
 */
public class MultiUsernameEditText extends WPEditText {

    private OnSelectionChangeListener mOnSelectionChangeListener;


    public MultiUsernameEditText(Context context) {
        super(context);
    }

    public MultiUsernameEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiUsernameEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        if (mOnSelectionChangeListener != null)
            mOnSelectionChangeListener.onSelectionChanged(selStart, selEnd);
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener onSelectionChangeListener) {
        this.mOnSelectionChangeListener = onSelectionChangeListener;
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selStart, int selEnd);
    }

}
