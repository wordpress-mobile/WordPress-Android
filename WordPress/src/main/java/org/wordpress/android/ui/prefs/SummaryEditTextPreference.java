package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;

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

public class SummaryEditTextPreference extends EditTextPreference
        implements SiteSettingsFragment.HasHint {
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
    public void onBindView(@NonNull View view) {
        super.onBindView(view);

        TextView summary = (TextView) view.findViewById(android.R.id.summary);
        if (summary != null) {
            summary.setEllipsize(TextUtils.TruncateAt.END);
            if (mLines != -1) summary.setLines(mLines);
            if (mMaxLines != -1) summary.setMaxLines(mMaxLines);
        }
    }

    @Override
    public boolean hasHint() {
        return !TextUtils.isEmpty(mHint);
    }

    @Override
    public String getHintText() {
        return mHint;
    }
}
