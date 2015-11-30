package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
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
    private AlertDialog mDialog;

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
    protected void showDialog(Bundle state) {
        Context context = getContext();
        Resources res = context.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Calypso_AlertDialog);
        View titleView = View.inflate(getContext(), R.layout.detail_list_preference_title, null);

        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(res.getString(R.string.cancel).toUpperCase(), this);
        if (titleView != null) {
            TextView titleText = (TextView) titleView.findViewById(R.id.title);
            if (titleText != null) {
                titleText.setText(getTitle());
            }

            builder.setCustomTitle(titleView);
        } else {
            builder.setTitle(getTitle());
        }

        View view = View.inflate(getContext(), getDialogLayoutResource(), null);
        if (view != null) {
            onBindDialogView(view);
            builder.setView(view);
        }

        if ((mDialog = builder.create()) == null) return;

        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.setOnDismissListener(this);
        mDialog.show();

        Button positive = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        Typeface typeface = TypefaceCache.getTypeface(getContext(),
                TypefaceCache.FAMILY_OPEN_SANS,
                Typeface.BOLD,
                TypefaceCache.VARIATION_LIGHT);

        if (positive != null) {
            //noinspection deprecation
            positive.setTextColor(res.getColor(R.color.blue_medium));
            positive.setTypeface(typeface);
        }

        if (negative != null) {
            //noinspection deprecation
            negative.setTextColor(res.getColor(R.color.blue_medium));
            negative.setTypeface(typeface);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        if (view != null) {
            Typeface typeface = TypefaceCache.getTypeface(getContext(),
                    TypefaceCache.FAMILY_OPEN_SANS,
                    Typeface.NORMAL,
                    TypefaceCache.VARIATION_NORMAL);
            EditText editText = getEditText();
            ViewParent oldParent = editText.getParent();
            if (oldParent != view) {
                if (oldParent != null) {
                    ((ViewGroup) oldParent).removeView(editText);
                }
                ((View) oldParent).setPadding(((View) oldParent).getPaddingLeft(), 0, ((View) oldParent).getPaddingRight(), ((View) oldParent).getPaddingBottom());
                onAddEditTextToDialogView(view, editText);
            }
            editText.setSelection(editText.getText().length());
            editText.setTypeface(typeface);
            editText.setTextColor(getContext().getResources().getColor(R.color.grey_dark));
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
        onDialogClosed(false);
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
