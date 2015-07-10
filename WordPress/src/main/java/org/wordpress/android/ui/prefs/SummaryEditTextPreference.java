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
 * Re: {@link SummaryPreference}
 */

public class SummaryEditTextPreference extends EditTextPreference {
    private int mLines;
    private int mMaxLines;

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
}
