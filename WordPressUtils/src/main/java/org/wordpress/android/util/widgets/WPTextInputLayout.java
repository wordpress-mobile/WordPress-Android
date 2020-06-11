package org.wordpress.android.util.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import org.wordpress.android.util.R;

/**
 * Custom TextInputLayout to provide a usable getBaseline() and error view padding
 */
public class WPTextInputLayout extends TextInputLayout {
    public WPTextInputLayout(Context context) {
        super(context);
    }

    public WPTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getBaseline() {
        EditText editText = getEditText();
        return editText != null ? editText.getBaseline() - editText.getPaddingBottom()
                                  + getResources().getDimensionPixelSize(R.dimen.textinputlayout_baseline_correction)
                : 0;
    }

    @Override
    public void setErrorEnabled(boolean enabled) {
        super.setErrorEnabled(enabled);

        // remove hardcoded side padding of the error view
        if (enabled) {
            View errorView = findViewById(com.google.android.material.R.id.textinput_error);
            if (errorView != null && errorView.getParent() != null) {
                ((View) errorView.getParent())
                        .setPadding(0, errorView.getPaddingTop(), 0, errorView.getPaddingBottom());
            }
        }
    }
}
