package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

import org.wordpress.android.R;

public class AutoSaveEditText extends EditText {
    private AutoSaveTextHelper mAutoSaveTextHelper = new AutoSaveTextHelper();
    private Boolean mEnabled;

    public AutoSaveEditText(Context context) {
        super(context, null);
    }

    public AutoSaveEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        readCustomAttrs(context, attrs);
    }

    public AutoSaveEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readCustomAttrs(context, attrs);
    }

    public AutoSaveTextHelper getAutoSaveTextHelper() {
        return mAutoSaveTextHelper;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        load();
    }

    @Override protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        // skip save on first call (during the constructor call)
        if (text.length() == 0 && lengthBefore == 0) {
            return;
        }
        save();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        save();
    }

    private void load() {
        if (!mEnabled) {
            return;
        }
        String text = getAutoSaveTextHelper().loadString(this);
        if (!text.isEmpty()) {
            setText(text);
            setSelection(text.length());
        }
    }

    private void save() {
        if (!mEnabled || getText() == null) {
            return;
        }
        getAutoSaveTextHelper().saveString(this, getText().toString());
    }

    private void readCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AutoSaveEditText, 0, 0);
        if (array != null) {
            mEnabled = array.getBoolean(R.styleable.AutoSaveEditText_autoSaveEnabled, false);
        }
    }
}
