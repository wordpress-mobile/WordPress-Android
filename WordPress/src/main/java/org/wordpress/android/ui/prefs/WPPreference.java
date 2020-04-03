package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;

import org.wordpress.android.R;

public class WPPreference extends Preference implements PreferenceHint {
    private String mHint;

    public WPPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DetailListPreference);

        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.DetailListPreference_longClickHint) {
                mHint = array.getString(index);
            }
        }

        array.recycle();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        Resources res = getContext().getResources();
        TextView titleView = view.findViewById(android.R.id.title);
        TextView summaryView = view.findViewById(android.R.id.summary);
        if (titleView != null) {
            TextViewCompat.setTextAppearance(titleView, R.style.TextAppearance_MaterialComponents_Subtitle1);
            if (!isEnabled()) {
                titleView.setAlpha(ResourcesCompat.getFloat(res, R.dimen.material_emphasis_disabled));
            } else {
                titleView.setAlpha(1f);
            }
        }
        if (summaryView != null) {
            TextViewCompat.setTextAppearance(summaryView, R.style.TextAppearance_MaterialComponents_Body2);
            if (!isEnabled()) {
                summaryView.setAlpha(ResourcesCompat.getFloat(res, R.dimen.material_emphasis_disabled));
            } else {
                summaryView.setAlpha(ResourcesCompat.getFloat(res, R.dimen.material_emphasis_medium));
            }
        }
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
