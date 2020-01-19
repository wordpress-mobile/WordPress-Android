package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;

import java.util.Locale;

/**
 * Standard EditTextPreference that has attributes to limit summary length.
 * <p>
 * Created for and used by {@link SiteSettingsFragment} to style some Preferences.
 * <p>
 * When declaring this class in a layout file you can use the following attributes:
 * - app:summaryLines : sets the number of lines to display in the Summary field
 * (see {@link TextView#setLines(int)} for details)
 * - app:maxSummaryLines : sets the maximum number of lines the Summary field can display
 * (see {@link TextView#setMaxLines(int)} for details)
 * - app:longClickHint : sets the string to be shown in a Toast when preference is long clicked
 * - app:dialogSummary : sets the summary text in the dialog
 */

public class SummaryEditTextPreference extends EditTextPreference implements PreferenceHint {
    private int mLines;
    private int mMaxLines;
    private String mHint;
    private String mDialogMessage;
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
            } else if (index == R.styleable.SummaryEditTextPreference_dialogMessage) {
                mDialogMessage = array.getString(index);
            }
        }

        array.recycle();
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        TextView summaryView = view.findViewById(android.R.id.summary);
        if (summaryView != null) {
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View titleView = View.inflate(getContext(), R.layout.detail_list_preference_title, null);
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;

        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(res.getString(android.R.string.cancel).toUpperCase(Locale.getDefault()), this);
        if (titleView != null) {
            TextView titleText = titleView.findViewById(R.id.title);
            if (titleText != null) {
                titleText.setText(getDialogTitle());
            }

            builder.setCustomTitle(titleView);
        } else {
            builder.setTitle(getTitle());
        }

        if (mDialogMessage != null) {
            builder.setMessage(mDialogMessage);
        }

        View view = View.inflate(getContext(), getDialogLayoutResource(), null);
        if (view != null) {
            onBindDialogView(view);
            builder.setView(view);
        }

        mDialog = builder.create();

        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.setOnDismissListener(this);
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mDialog.show();
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);
        if (view == null) return;

        EditText editText = getEditText();
        ViewParent oldParent = editText.getParent();
        if (oldParent != view) {
            if (oldParent instanceof ViewGroup) {
                ViewGroup groupParent = (ViewGroup) oldParent;
                groupParent.removeView(editText);
                ViewCompat.setPaddingRelative(groupParent, ViewCompat.getPaddingStart(groupParent), 0,
                        ViewCompat.getPaddingEnd(groupParent), groupParent.getPaddingBottom());
            }
            onAddEditTextToDialogView(view, editText);
        }
        editText.setSelection(editText.getText().length());
        // RtL language support
        editText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

        TextView message = view.findViewById(android.R.id.message);

        // Dialog message has some extra bottom margin we don't want
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) message.getLayoutParams();
        int leftMargin = 0;
        int bottomMargin = view.getResources().getDimensionPixelSize(R.dimen.margin_small);
        // Different versions handle the message view's margin differently
        // This is a small hack to try to make it align with the input for earlier versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            leftMargin = view.getResources().getDimensionPixelSize(R.dimen.margin_small);
        }
        layoutParams.setMargins(0, layoutParams.topMargin, 0, bottomMargin);
        MarginLayoutParamsCompat.setMarginStart(layoutParams, leftMargin);
        MarginLayoutParamsCompat.setMarginEnd(layoutParams, layoutParams.rightMargin);

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
