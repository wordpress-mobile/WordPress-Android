package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import org.wordpress.android.R;

public class WPSwitchPreference extends SwitchPreference implements PreferenceHint {
    private String mHint;
    private ColorStateList mTint;
    private ColorStateList mThumbTint;
    private int mStartOffset = 0;

    public WPSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SummaryEditTextPreference);
        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.SummaryEditTextPreference_longClickHint) {
                mHint = array.getString(index);
            } else if (index == R.styleable.SummaryEditTextPreference_iconTint) {
                int resourceId = array.getResourceId(index, 0);
                if (resourceId != 0) {
                    mTint = AppCompatResources.getColorStateList(context, resourceId);
                }
            } else if (index == R.styleable.SummaryEditTextPreference_switchThumbTint) {
                mThumbTint = array.getColorStateList(index);
            } else if (index == R.styleable.SummaryEditTextPreference_startOffset) {
                mStartOffset = array.getDimensionPixelSize(index, 0);
            }
        }

        array.recycle();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        ImageView icon = view.findViewById(android.R.id.icon);
        if (icon != null && mTint != null) {
            icon.setImageTintList(mTint);
        }

        TextView titleView = view.findViewById(android.R.id.title);
        if (titleView != null) {
            Resources res = getContext().getResources();

            // add padding to the start of nested preferences
            if (!TextUtils.isEmpty(getDependency())) {
                int margin = res.getDimensionPixelSize(R.dimen.margin_large);
                ViewCompat.setPaddingRelative(titleView, margin + mStartOffset, 0, 0, 0);
            } else {
                ViewCompat.setPaddingRelative(titleView, mStartOffset, 0, 0, 0);
            }
        }

        // style custom switch preference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Switch switchControl = getSwitch((ViewGroup) view);
            if (switchControl != null) {
                if (mThumbTint != null) {
                    switchControl.setThumbTintList(mThumbTint);
                }
            }
        }

        // Add padding to start of switch.
        ViewCompat.setPaddingRelative(getSwitch((ViewGroup) view),
                getContext().getResources().getDimensionPixelSize(R.dimen.margin_extra_large), 0, 0, 0);
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
