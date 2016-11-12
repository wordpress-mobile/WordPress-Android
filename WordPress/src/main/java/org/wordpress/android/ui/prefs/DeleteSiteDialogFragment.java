package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.util.AnalyticsUtils;

public class DeleteSiteDialogFragment extends DialogFragment implements TextWatcher, DialogInterface.OnShowListener {
    public static final String SITE_DOMAIN_KEY = "site-domain";

    private AlertDialog mDeleteSiteDialog;
    private EditText mUrlConfirmation;
    private Button mDeleteButton;
    private String mSiteDomain = "";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AnalyticsUtils.trackWithCurrentBlogDetails(
                AnalyticsTracker.Stat.SITE_SETTINGS_DELETE_SITE_ACCESSED);
        retrieveSiteDomain();
        configureAlertViewBuilder(builder);

        mDeleteSiteDialog = builder.create();
        mDeleteSiteDialog.setOnShowListener(this);

        return mDeleteSiteDialog;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (isUrlConfirmationTextValid()) {
            mDeleteButton.setEnabled(true);
        } else {
            mDeleteButton.setEnabled(false);
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        mDeleteButton = mDeleteSiteDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        mDeleteButton.setEnabled(false);
    }

    private void configureAlertViewBuilder(AlertDialog.Builder builder) {
        builder.setTitle(R.string.confirm_delete_site);
        builder.setMessage(confirmationPromptString());

        configureUrlConfirmation(builder);
        configureButtons(builder);
    }

    private void configureButtons(AlertDialog.Builder builder) {
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Fragment target = getTargetFragment();
                if (target != null) {
                    target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
                }

                dismiss();
            }
        });
    }

    private Spannable confirmationPromptString() {
        String deletePrompt = String.format(getString(R.string.confirm_delete_site_prompt), mSiteDomain);
        Spannable promptSpannable = new SpannableString(deletePrompt);
        int beginning = deletePrompt.indexOf(mSiteDomain);
        int end = beginning + mSiteDomain.length();
        promptSpannable.setSpan(new StyleSpan(Typeface.BOLD), beginning, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return promptSpannable;
    }

    private void configureUrlConfirmation(AlertDialog.Builder builder) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.delete_site_dialog, null);
        mUrlConfirmation = (EditText) view.findViewById(R.id.url_confirmation);
        mUrlConfirmation.addTextChangedListener(this);
        builder.setView(view);
    }

    private void retrieveSiteDomain() {
        Bundle args = getArguments();
        mSiteDomain = getString(R.string.wordpress_dot_com).toLowerCase();
        if (args != null) {
            mSiteDomain = args.getString(SITE_DOMAIN_KEY);
        }
    }

    private boolean isUrlConfirmationTextValid() {
        String confirmationText = mUrlConfirmation.getText().toString().trim().toLowerCase();
        String hintText = mSiteDomain.toLowerCase();

        return confirmationText.equals(hintText);
    }
}
