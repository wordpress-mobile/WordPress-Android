package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.WPPrefUtils;

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
    private int mWhichButtonClicked;

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

        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);

        if (titleView != null) WPPrefUtils.layoutAsSubhead(titleView);

        if (summaryView != null) {
            WPPrefUtils.layoutAsBody1(summaryView);
            summaryView.setEllipsize(TextUtils.TruncateAt.END);
            summaryView.setInputType(getEditText().getInputType());
            if (mLines != -1) summaryView.setLines(mLines);
            if (mMaxLines != -1) summaryView.setMaxLines(mMaxLines);
        }
    }

    @Override
    public Dialog getDialog() {
        return mDialog;
    }

    @Override
    protected void showDialog(Bundle state) {
        Context context = getContext();
        Resources res = context.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Calypso_AlertDialog);
        View titleView = View.inflate(getContext(), R.layout.detail_list_preference_title, null);
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;

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
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mDialog.show();

        Button positive = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positive != null) WPPrefUtils.layoutAsFlatButton(positive);
        if (negative != null) WPPrefUtils.layoutAsFlatButton(negative);
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);
        if (view == null) return;

        EditText editText = getEditText();
        ViewParent oldParent = editText.getParent();
        if (oldParent != view) {
            if (oldParent != null && oldParent instanceof ViewGroup) {
                ViewGroup groupParent = (ViewGroup) oldParent;
                groupParent.removeView(editText);
                groupParent.setPadding(groupParent.getPaddingLeft(), 0, groupParent.getPaddingRight(), groupParent.getPaddingBottom());
            }
            onAddEditTextToDialogView(view, editText);
        }
        WPPrefUtils.layoutAsInput(editText);
        editText.setSelection(editText.getText().length());

        TextView message = (TextView) view.findViewById(android.R.id.message);
        WPPrefUtils.layoutAsDialogMessage(message);

        // Dialog message has some extra bottom margin we don't want
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) message.getLayoutParams();
        int leftMargin = 0;
        int bottomMargin = view.getResources().getDimensionPixelSize(R.dimen.margin_small);
        // Different versions handle the message view's margin differently
        // This is a small hack to try to make it align with the input for earlier versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            leftMargin = view.getResources().getDimensionPixelSize(R.dimen.margin_small);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            leftMargin = view.getResources().getDimensionPixelSize(R.dimen.margin_large);
        }
        layoutParams.setMargins(leftMargin, layoutParams.topMargin, layoutParams.rightMargin, bottomMargin);
        message.setLayoutParams(layoutParams);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            callChangeListener(getEditText().getText());
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
