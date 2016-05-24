package org.wordpress.android.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public class LinkDialogFragment extends DialogFragment {

    public static final int LINK_DIALOG_REQUEST_CODE_ADD = 1;
    public static final int LINK_DIALOG_REQUEST_CODE_UPDATE = 2;
    public static final int LINK_DIALOG_REQUEST_CODE_DELETE = 3;

    public static final String LINK_DIALOG_ARG_URL  = "linkURL";
    public static final String LINK_DIALOG_ARG_TEXT = "linkText";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_link, null);

        final EditText urlEditText = (EditText) view.findViewById(R.id.linkURL);
        final EditText linkEditText = (EditText) view.findViewById(R.id.linkText);

        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        intent.putExtra(LINK_DIALOG_ARG_URL, urlEditText.getText().toString());
                        intent.putExtra(LINK_DIALOG_ARG_TEXT, linkEditText.getText().toString());
                        getTargetFragment().onActivityResult(getTargetRequestCode(), getTargetRequestCode(), intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        LinkDialogFragment.this.getDialog().cancel();
                    }
                });

        // If updating an existing link, add a 'Delete' button
        if (getTargetRequestCode() == LINK_DIALOG_REQUEST_CODE_UPDATE) {
            builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getTargetFragment().onActivityResult(getTargetRequestCode(), LINK_DIALOG_REQUEST_CODE_DELETE, null);
                }
            });
        }

        // Prepare initial state of EditTexts
        Bundle bundle = getArguments();
        if (bundle != null) {
            linkEditText.setText(bundle.getString(LINK_DIALOG_ARG_TEXT));

            String url = bundle.getString(LINK_DIALOG_ARG_URL);
            if (url != null) {
                urlEditText.setText(url);
            }
            urlEditText.selectAll();
        }

        AlertDialog dialog =  builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }
}
