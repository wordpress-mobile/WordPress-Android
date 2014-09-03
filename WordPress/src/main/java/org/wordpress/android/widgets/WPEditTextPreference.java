package org.wordpress.android.widgets;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class WPEditTextPreference extends EditTextPreference {
    public WPEditTextPreference(Context context) {
        super(context);
    }

    public WPEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WPEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult) {
            callChangeListener(null);
        }
    }
}

