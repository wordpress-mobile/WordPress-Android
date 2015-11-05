package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.EditTextPreference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.widgets.TypefaceCache;

/**
 * Standard EditTextPreference that has attributes to limit summary length.
 *
 * Created for and used by {@link SiteSettingsFragment} to style some Preferences.
 *
 * When declaring this class in a layout file you can use the following attributes:
 *  - app:summaryLines : sets the number of lines to display in the Summary field
 *                       (see {@link TextView#setLines(int)} for details)
 *  - app:maxSummaryLines : sets the maximum number of lines the Summary field can display
 *                       (see {@link TextView#setMaxLines(int)} for details)
 *  - app:longClickHint : sets the string to be shown in a Toast when preference is long clicked
 */

public class SummaryEditTextPreference extends EditTextPreference implements PreferenceHint {
    private int mLines;
    private int mMaxLines;
    private String mHint;

    public SummaryEditTextPreference(Context context) {
        super(context);
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLines = -1;
        mMaxLines = -1;

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SummaryEditTextPreference);

        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.SummaryEditTextPreference_summaryLines) {
                mLines = array.getInt(index, -1);
            } else if (index == R.styleable.SummaryEditTextPreference_maxSummaryLines) {
                mMaxLines = array.getInt(index, -1);
            } else if (index == R.styleable.SummaryEditTextPreference_longClickHint) {
                mHint = array.getString(index);
            }
        }

        array.recycle();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        Resources res = getContext().getResources();
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        Typeface font = TypefaceCache.getTypeface(getContext(),
                TypefaceCache.FAMILY_OPEN_SANS,
                Typeface.NORMAL,
                TypefaceCache.VARIATION_NORMAL);

        if (titleView != null) {
            if (isEnabled()) {
                titleView.setTextColor(res.getColor(R.color.grey_dark));
            } else {
                titleView.setTextColor(res.getColor(R.color.grey_lighten_10));
            }
            titleView.setTextSize(16);
            titleView.setTypeface(font);
        }

        if (summaryView != null) {
            if (isEnabled()) {
                summaryView.setTextColor(res.getColor(R.color.grey_darken_10));
            } else {
                summaryView.setTextColor(res.getColor(R.color.grey_lighten_10));
            }
            summaryView.setInputType(getEditText().getInputType());
            summaryView.setTextSize(14);
            summaryView.setTypeface(font);
            summaryView.setEllipsize(TextUtils.TruncateAt.END);
            if (mLines != -1) summaryView.setLines(mLines);
            if (mMaxLines != -1) summaryView.setMaxLines(mMaxLines);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        if (view != null) {
            EditText text = (EditText) view.findViewById(android.R.id.edit);
            if (text != null) {
                text.setSelection(text.getText().length());
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        ActivityUtils.hideKeyboard((Activity) getContext());
    }

    @Override
    public boolean hasHint() {
        return !TextUtils.isEmpty(mHint);
    }

    @Override
    public String getHint() {
        return mHint;
    }

    @Override
    public void setHint(String hint) {
        mHint = hint;
    }
}
