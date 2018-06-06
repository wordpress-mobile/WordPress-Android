package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import org.wordpress.android.R;

public class WPSwitchPreference extends SwitchPreference implements PreferenceHint {
    private String mHint;

    public WPSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SummaryEditTextPreference);
        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.SummaryEditTextPreference_longClickHint) {
                mHint = array.getString(index);
            }
        }

        array.recycle();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        TextView titleView = view.findViewById(android.R.id.title);
        if (titleView != null) {
            Resources res = getContext().getResources();
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.text_sz_large));
            titleView.setTextColor(res.getColor(isEnabled() ? R.color.grey_dark : R.color.grey_lighten_10));

            // add padding to the start of nested preferences
            if (!TextUtils.isEmpty(getDependency())) {
                ViewCompat.setPaddingRelative(titleView, res.getDimensionPixelSize(R.dimen.margin_large), 0, 0, 0);
            }
        }

        // style custom switch preference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Switch switchControl = getSwitch((ViewGroup) view);
            if (switchControl != null) {
                switchControl.setThumbTintList(ContextCompat.getColorStateList(this.getContext(),
                        R.color.dialog_compound_button_thumb));
                switchControl.setTrackTintList(ContextCompat.getColorStateList(this.getContext(),
                        R.color.dialog_compound_button_track));
            }
        }
    }

    private Switch getSwitch(ViewGroup parentView) {
        for (int i = 0; i < parentView.getChildCount(); i++) {
            View childView = parentView.getChildAt(i);

            if (childView instanceof Switch) {
                return (Switch) childView;
            } else if (childView instanceof ViewGroup) {
                Switch theSwitch = getSwitch((ViewGroup) childView);
                if (theSwitch != null) {
                    return theSwitch;
                }
            }
        }
        return null;
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
