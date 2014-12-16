package org.wordpress.persistentedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.EditText;

public class PersistentEditText extends EditText {
    private PersistentEditTextHelper mPersistentEditTextHelper;
    private Boolean mEnabled;

    public PersistentEditText(Context context) {
        super(context, null);
        mPersistentEditTextHelper = new PersistentEditTextHelper(context);
    }

    public PersistentEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        readCustomAttrs(context, attrs);
        mPersistentEditTextHelper = new PersistentEditTextHelper(context);
    }

    public PersistentEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readCustomAttrs(context, attrs);
        mPersistentEditTextHelper = new PersistentEditTextHelper(context);
    }

    public PersistentEditTextHelper getAutoSaveTextHelper() {
        return mPersistentEditTextHelper;
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
        getAutoSaveTextHelper().loadString(this);
    }

    private void save() {
        if (!mEnabled) {
            return;
        }
        getAutoSaveTextHelper().saveString(this);
    }

    private void readCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PersistentEditText, 0, 0);
        if (array != null) {
            mEnabled = array.getBoolean(R.styleable.PersistentEditText_persistenceEnabled, false);
        }
    }
}
