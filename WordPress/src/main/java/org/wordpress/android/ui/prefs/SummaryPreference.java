package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * Used as a convenience to give custom preferences attributes for summary line length.
 */

public class SummaryPreference extends Preference {
    private int mLines;
    private int mMaxLines;

    public SummaryPreference(Context context) {
        super(context);
    }

    public SummaryPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mLines = -1;
        mMaxLines = -1;

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SummaryPreference);

        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.SummaryPreference_summaryLines) {
                mLines = array.getInt(index, -1);
            } else if (index == R.styleable.SummaryPreference_maxSummaryLines) {
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
            if (mLines != -1) summary.setLines(mLines);
            if (mMaxLines != -1) summary.setMaxLines(mMaxLines);
        }
    }
}
