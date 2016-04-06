package org.wordpress.android.ui.prefs;

import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditTextPreferenceWithValidation extends SummaryEditTextPreference {
    private ValidationType mValidationType = ValidationType.NONE;

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

    public enum ValidationType {
        NONE, EMAIL, URL
    }
}
