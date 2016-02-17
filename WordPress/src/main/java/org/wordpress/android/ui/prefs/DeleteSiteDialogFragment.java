package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;

public class DeleteSiteDialogFragment extends DialogFragment implements TextWatcher, DialogInterface.OnShowListener {
    public static final String SITE_DOMAIN_KEY = "site-domain";

    private AlertDialog mDeleteSiteDialog;
    private EditText mPasswordConfirmation;
    private Button mDeleteButton;
    private String mPassword;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        configureAlertViewBuilder(builder);

        mDeleteSiteDialog = builder.create();
        mDeleteSiteDialog.setOnShowListener(this);
        mPassword = WordPress.getCurrentBlog().getDotcom_password();


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
        builder.setTitle(R.string.delete_site_question);
        String deletePrompt = String.format(getString(R.string.confirm_delete_site_prompt), getSiteDomainText());
        builder.setMessage(deletePrompt);

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

    private void configureUrlConfirmation(AlertDialog.Builder builder) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.delete_site_dialog, null);
        mPasswordConfirmation = (EditText) view.findViewById(R.id.password_confirmation);
        mPasswordConfirmation.setHint(R.string.password_hint);
        mPasswordConfirmation.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordConfirmation.addTextChangedListener(this);
        builder.setView(view);
    }

    private String getSiteDomainText() {
        Bundle args = getArguments();
        String siteDomain = getString(R.string.delete).toLowerCase();
        if (args != null) {
            siteDomain = args.getString(SITE_DOMAIN_KEY);
        }

        return String.format(getString(R.string.confirm_delete_site_prompt), siteDomain);
    }

    private boolean isUrlConfirmationTextValid() {
        String confirmationText = mPasswordConfirmation.getText().toString().trim().toLowerCase();
        String hintText = mPassword.toLowerCase();

        return confirmationText.equals(hintText);
    }
}
