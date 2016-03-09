package org.wordpress.android.util.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

/*
 * @deprecated This custom EditText is used solely by the "legacy" editor in WP Android.
 * It will be removed when we drop the legacy editor and should not be used in new code.
 */
@Deprecated
public class WPEditText extends EditText {
    private EditTextImeBackListener mOnImeBack;
    private OnSelectionChangedListener onSelectionChangedListener;

    public WPEditText(Context context) {
        super(context);
    }

    public WPEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged();
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (mOnImeBack != null)
                mOnImeBack.onImeBack(this, this.getText().toString());
        }

        return super.onKeyPreIme(keyCode, event);
    }

    public void setOnEditTextImeBackListener(EditTextImeBackListener listener) {
        mOnImeBack = listener;
    }

    public interface EditTextImeBackListener {
        public abstract void onImeBack(WPEditText ctrl, String text);
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        onSelectionChangedListener = listener;
    }

    public interface OnSelectionChangedListener {
        public abstract void onSelectionChanged();
    }
}
