package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.util.ToastUtils;

public class DeleteSiteDialogFragment extends DialogFragment implements TextWatcher, DialogInterface.OnShowListener {
    private AlertDialog mDeleteSiteDialog;
    private EditText mUrlConfirmation;
    private Button mDeleteButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Confirm by entering the primary domain below.");
        builder.setTitle("Delete the entire site?");

        View view = getActivity().getLayoutInflater().inflate(R.layout.delete_site_dialog, null);
        mUrlConfirmation = (EditText) view.findViewById(R.id.url_confirmation);
        mUrlConfirmation.setHint("kwonye.wordpress.com");

        mUrlConfirmation.addTextChangedListener(this);

        builder.setView(view);
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mUrlConfirmation.getText().toString().toLowerCase().equals(mUrlConfirmation.getHint().toString().toLowerCase())) {

                } else {
                    ToastUtils.showToast(getActivity(), "Please type the correct domain name");
                }
            }
        });

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
        if (mUrlConfirmation.getText().toString().toLowerCase().equals(mUrlConfirmation.getHint().toString().toLowerCase())) {
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
}
