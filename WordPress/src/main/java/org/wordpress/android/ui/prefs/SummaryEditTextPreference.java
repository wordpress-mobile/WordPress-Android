package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
    private static final String STATE_TEXT = "SummaryEditTextPreference_STATE_TEXT";
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

            EditText editText = getEditText();
            if (editText != null && state.containsKey(STATE_TEXT)) {
                editText.setText(state.getString(STATE_TEXT));
                editText.setSelection(editText.getText().length());
            }
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

    // region adapted from DialogPreference and EditTextPreference to make sure dialog state behaves correctly,
    // specially in system initiated death/recreation scenarios, such as changing system dark mode.
    private void dismissDialog() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }

        mDialog.dismiss();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mDialog.onSaveInstanceState();

        // Save the text from the EditText
        CharSequence text = getEditText().getText();
        String stateText = text == null ? "" : text.toString();
        myState.dialogBundle.putString(STATE_TEXT, stateText);

        // Since dialog is showing, let's dismiss it so it doesn't leak. This is not the best place to do it, but
        // since the android.preference is deprecated we are not able to register the proper lifecycle listeners, and
        // we should migrate to androidx.preference or something similar in the future.
        dismissDialog();

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final @NonNull Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        public boolean isDialogShowing;
        public Bundle dialogBundle;

        SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle(getClass().getClassLoader());
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }
    }
    // endregion
}
