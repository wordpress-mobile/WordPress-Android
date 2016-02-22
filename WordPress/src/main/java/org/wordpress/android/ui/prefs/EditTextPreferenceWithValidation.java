package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.util.WPPrefUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditTextPreferenceWithValidation extends EditTextPreference {
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
    protected void showDialog(Bundle state)
    {
        super.showDialog(state);

        final AlertDialog dialog = (AlertDialog) getDialog();
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
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
            WPPrefUtils.layoutAsFlatButton(positiveButton);
        }
        if (negativeButton != null) WPPrefUtils.layoutAsFlatButton(negativeButton);
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
