package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.util.ValidationUtils;

public class EditTextPreferenceWithValidation extends SummaryEditTextPreference {
    private ValidationType mValidationType = ValidationType.NONE;
    // Ignore the default value, such as "Not Set", while showing the dialog
    private String mStringToIgnoreForPrefilling = "";

    public EditTextPreferenceWithValidation(Context context) {
        super(context);
    }

    public EditTextPreferenceWithValidation(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditTextPreferenceWithValidation(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        final AlertDialog dialog = (AlertDialog) getDialog();
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String error = null;
                    CharSequence text = getEditText().getText();
                    if (mValidationType == ValidationType.EMAIL) {
                        error = ValidationUtils.validateEmail(text) ? null
                                : getContext().getString(R.string.invalid_email_message);
                    } else if (!TextUtils.isEmpty(text)) {
                        if (mValidationType == ValidationType.URL) {
                            error = ValidationUtils.validateUrl(text) ? null
                                    : getContext().getString(R.string.invalid_url_message);
                        } else if (mValidationType == ValidationType.PASSWORD) {
                            error = ValidationUtils.validatePassword(text) ? null
                                    : getContext().getString(R.string.change_password_invalid_message);
                        }
                    }

                    if (error != null) {
                        getEditText().setError(error);
                    } else {
                        callChangeListener(text);
                        dialog.dismiss();
                    }
                }
            });
        }

        CharSequence summary = getSummary();
        if (summary == null || summary.equals(mStringToIgnoreForPrefilling)) {
            getEditText().setText("");
        } else {
            getEditText().setText(summary);
            getEditText().setSelection(0, summary.length());
        }

        // clear previous errors
        getEditText().setError(null);
    }

    public void setValidationType(ValidationType validationType) {
        mValidationType = validationType;
    }

    public void setStringToIgnoreForPrefilling(String stringToIgnoreForPrefilling) {
        mStringToIgnoreForPrefilling = stringToIgnoreForPrefilling;
    }

    public enum ValidationType {
        NONE, EMAIL, PASSWORD, URL
    }
}
