package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditTextPreferenceWithValidation extends SummaryEditTextPreference {
    private ValidationType mValidationType = ValidationType.NONE;
    private EditText mEditText;
    private String mHint;

    public EditTextPreferenceWithValidation(Context context) {
        super(context);
        setDialogLayoutResource(R.layout.edit_text_preference_with_validation_dialog);
    }

    public EditTextPreferenceWithValidation(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDialogLayoutResource(R.layout.edit_text_preference_with_validation_dialog);
    }

    public EditTextPreferenceWithValidation(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.edit_text_preference_with_validation_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mEditText = (EditText) view.findViewById(R.id.dialog_edit_text);
        mEditText.setInputType(super.getEditText().getInputType());
        WPTextView hintTextView = (WPTextView) view.findViewById(R.id.dialog_hint);
        if (mHint != null) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(mHint);
        }
        else {
            hintTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public EditText getEditText() {
        if (mEditText != null) {
            return mEditText;
        }
        return super.getEditText();
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
                        error = validateEmail(text);
                    } else if (!TextUtils.isEmpty(text) && mValidationType == ValidationType.URL) {
                        error = validateUrl(text);
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
        if (TextUtils.isEmpty(summary)) {
            getEditText().setText("");
        } else {
            getEditText().setText(summary);
            getEditText().setSelection(0, summary.length());
        }

        // clear previous errors
        getEditText().setError(null);
    }

    private String validateEmail(CharSequence text) {
        final Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(text);
        if (!matcher.matches()) {
            return getContext().getString(R.string.invalid_email_message);
        }
        return null;
    }

    private String validateUrl(CharSequence text) {
        final Pattern urlRegExPattern = Patterns.WEB_URL;
        Matcher matcher = urlRegExPattern.matcher(text);
        if (!matcher.matches()) {
            return getContext().getString(R.string.invalid_url_message);
        }
        return null;
    }

    public void setValidationType(ValidationType validationType) {
        mValidationType = validationType;
    }

    public void setHint(String hint) {
        mHint = hint;
    }

    public enum ValidationType {
        NONE, EMAIL, URL
    }
}
