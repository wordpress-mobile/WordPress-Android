package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import org.wordpress.android.R;
import org.wordpress.android.util.ContextExtensionsKt;

public class WPSwitchPreference extends SwitchPreference implements PreferenceHint {
    private String mHint;
    private @ColorRes int mTint = 0;
    private ColorStateList mThumbTint;
    private ColorStateList mTrackTint;
    private @ColorRes int mTextColor = 0;
    private @ColorRes int mBackgroundCheckedColor = 0;
    private @ColorRes int mBackgroundUncheckedColor = 0;
    private int mStartOffset = 0;

    private View mContainer;

    public WPSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.SummaryEditTextPreference);
        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.SummaryEditTextPreference_longClickHint) {
                mHint = array.getString(index);
            } else if (index == R.styleable.SummaryEditTextPreference_iconTint) {
                mTint = array.getResourceId(index, R.color.neutral);
            } else if (index == R.styleable.SummaryEditTextPreference_switchThumbTint) {
                mThumbTint = array.getColorStateList(index);
            } else if (index == R.styleable.SummaryEditTextPreference_switchTrackTint) {
                mTrackTint = array.getColorStateList(index);
            } else if (index == R.styleable.SummaryEditTextPreference_preferenceTextColor) {
                mTextColor = array.getResourceId(index, android.R.color.white);
            } else if (index == R.styleable.SummaryEditTextPreference_backgroundColorChecked) {
                mBackgroundCheckedColor = array.getResourceId(index, android.R.color.white);
            } else if (index == R.styleable.SummaryEditTextPreference_backgroundColorUnchecked) {
                mBackgroundUncheckedColor = array.getResourceId(index, android.R.color.white);
            } else if (index == R.styleable.SummaryEditTextPreference_startOffset) {
                mStartOffset = array.getDimensionPixelSize(index, 0);
            }
        }

        array.recycle();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);
        mContainer = view;

        ImageView icon = view.findViewById(android.R.id.icon);
        if (icon != null && mTint != 0) {
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), mTint)));
        }

        TextView titleView = view.findViewById(android.R.id.title);
        TextView coloredTitleView = view.findViewById(R.id.colored_title);
        if (titleView != null && coloredTitleView != null) {
            Resources res = getContext().getResources();
            coloredTitleView.setText(titleView.getText());
            coloredTitleView.setVisibility(View.VISIBLE);
            titleView.setVisibility(View.GONE);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.text_sz_large));
            if (mTextColor == 0) {
                coloredTitleView.setTextColor(res.getColor(
                        isEnabled() ? ContextExtensionsKt.getColorResIdFromAttribute(getContext(), R.attr.wpColorText)
                                : R.color.neutral_20));
            } else {
                coloredTitleView.setTextColor(ContextCompat.getColor(this.getContext(), R.color.white));
            }

            // add padding to the start of nested preferences
            if (!TextUtils.isEmpty(getDependency())) {
                int margin = res.getDimensionPixelSize(R.dimen.margin_large);
                ViewCompat.setPaddingRelative(coloredTitleView, margin + mStartOffset, 0, 0, 0);
            } else {
                ViewCompat.setPaddingRelative(coloredTitleView, mStartOffset, 0, 0, 0);
            }
        }

        // style custom switch preference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Switch switchControl = getSwitch((ViewGroup) view);
            if (switchControl != null) {
                if (mThumbTint == null) {
                    switchControl.setThumbTintList(ContextCompat.getColorStateList(this.getContext(),
                            R.color.primary_40_gray_20_gray_40_selector));
                } else {
                    switchControl.setThumbTintList(mThumbTint);
                }
                if (mTrackTint == null) {
                    switchControl.setTrackTintList(ContextCompat.getColorStateList(this.getContext(),
                            R.color.primary_40_gray_90_gray_50_selector));
                } else {
                    switchControl.setTrackTintList(mTrackTint);
                }
                setBackground(switchControl.isChecked());
            }
        }

        // Add padding to start of switch.
        ViewCompat.setPaddingRelative(getSwitch((ViewGroup) view),
                getContext().getResources().getDimensionPixelSize(R.dimen.margin_extra_large), 0, 0, 0);
    }

    private void setBackground(boolean checked) {
        if (mContainer != null && mBackgroundCheckedColor != 0 && mBackgroundUncheckedColor != 0) {
            if (checked) {
                mContainer.setBackgroundColor(ContextCompat.getColor(this.getContext(), mBackgroundCheckedColor));
            } else {
                mContainer.setBackgroundColor(ContextCompat.getColor(this.getContext(), mBackgroundUncheckedColor));
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

    @Override public void setChecked(boolean checked) {
        super.setChecked(checked);
        setBackground(checked);
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
