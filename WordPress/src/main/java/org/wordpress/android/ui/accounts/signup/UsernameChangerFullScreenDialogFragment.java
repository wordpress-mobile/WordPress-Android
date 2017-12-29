package org.wordpress.android.ui.accounts.signup;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogContent;
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController;

public class UsernameChangerFullScreenDialogFragment extends Fragment implements FullScreenDialogContent {
    private TextInputEditText mUsername;

    protected FullScreenDialogController mDialogController;
    protected TextView mHeader;

    public static final String EXTRA_DISPLAY = "EXTRA_DISPLAY";
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String RESULT_USERNAME = "RESULT_USERNAME";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.username_changer_dialog_fragment, container, false);
    }

    @Override
    public void onViewCreated(final FullScreenDialogController controller) {
        this.mDialogController = controller;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mHeader = getView().findViewById(R.id.header);
        mHeader.setText(getHeaderText(getArguments().getString(EXTRA_USERNAME),
                getArguments().getString(EXTRA_DISPLAY)));

        mUsername = getView().findViewById(R.id.username);
        mUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    mHeader.setText(getHeaderText(getArguments().getString(EXTRA_USERNAME),
                            getArguments().getString(EXTRA_DISPLAY)));
                } else {
                    mHeader.setText(getHeaderText(s.toString(), getArguments().getString(EXTRA_DISPLAY)));
                }
            }
        });
    }

    @Override
    public boolean onConfirmClicked(FullScreenDialogController controller) {
        Bundle result = new Bundle();
        // TODO: Replace input text with selected list item.
        result.putString(RESULT_USERNAME, mUsername.getText().toString());
        controller.confirm(result);
        return true;
    }

    @Override
    public boolean onDismissClicked(FullScreenDialogController controller) {
        if (TextUtils.isEmpty(mUsername.getText().toString())) {
            mDialogController.dismiss();
        } else {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.username_changer_dismiss_message)
                    .setPositiveButton(R.string.username_changer_dismiss_button_positive,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mDialogController.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
        }

        return true;
    }

    protected Spanned getHeaderText(String username, String display) {
        return Html.fromHtml(
                String.format(
                        getString(R.string.username_changer_header),
                        "<b>",
                        username,
                        "</b>",
                        "<b>",
                        display,
                        "</b>"
                )
        );
    }
}
