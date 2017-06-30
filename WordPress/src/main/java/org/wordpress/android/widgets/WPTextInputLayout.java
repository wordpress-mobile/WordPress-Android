package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;

import org.wordpress.android.R;

/**
 * Custom TextInputLayout to provide a usable getBaseline()
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
    public int getBaseline()
    {
        EditText editText = getEditText();
        return editText != null ? editText.getBaseline() - editText.getPaddingBottom() + getResources().getDimensionPixelSize(R.dimen.textinputlayout_baseline_correction) : 0;
    }
}
