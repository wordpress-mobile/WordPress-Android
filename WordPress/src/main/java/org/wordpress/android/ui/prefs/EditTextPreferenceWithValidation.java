package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

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
        final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setOnClickListener(v -> {
                callChangeListener(getEditText().getText());
                dialog.dismiss();
            });


            getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    switch (mValidationType) {
                        case NONE:
                            break;
                        case EMAIL:
                            positiveButton.setEnabled(ValidationUtils.validateEmail(s));
                            break;
                        case PASSWORD:
                            positiveButton.setEnabled(ValidationUtils.validatePassword(s));
                            break;
                        case URL:
                            positiveButton.setEnabled(ValidationUtils.validateUrl(s));
                            break;
                        case PASSWORD_SELF_HOSTED:
                            positiveButton.setEnabled(ValidationUtils.validatePasswordSelfHosted(s));
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

        // Use "hidden" input type for passwords so characters are replaced with dots for added security.
        hideInputCharacters(
                mValidationType == ValidationType.PASSWORD || mValidationType == ValidationType.PASSWORD_SELF_HOSTED);
    }

    public void setValidationType(ValidationType validationType) {
        mValidationType = validationType;
    }

    public void setStringToIgnoreForPrefilling(String stringToIgnoreForPrefilling) {
        mStringToIgnoreForPrefilling = stringToIgnoreForPrefilling;
    }

    private void hideInputCharacters(boolean hide) {
        int selectionStart = getEditText().getSelectionStart();
        int selectionEnd = getEditText().getSelectionEnd();
        getEditText().setTransformationMethod(hide ? PasswordTransformationMethod.getInstance() : null);
        getEditText().setSelection(selectionStart, selectionEnd);
    }

    public enum ValidationType {
        NONE, EMAIL, PASSWORD, PASSWORD_SELF_HOSTED, URL
    }
}
