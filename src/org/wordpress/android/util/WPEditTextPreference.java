package org.wordpress.android.util;

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

        if (positiveResult) {
            if (getEditText() != null && getEditText().getText() != null) {
                String value = getEditText().getText().toString();
                if (callChangeListener(value)) {
                    setText(value);
                }
            }
        } else {
            callChangeListener(null);
        }
    }
}

