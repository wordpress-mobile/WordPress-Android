package org.wordpress.android.ui.people;

import org.wordpress.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;

public class UsernameRemoveDialogFragment extends DialogFragment {
    private static final String ARG_USERNAME = "ARG_USERNAME";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_MESSAGE = "ARG_MESSAGE";

    public interface UsernameRemover {
        void removeUsername(String username);
    }

    public static UsernameRemoveDialogFragment newInstance(String username, @StringRes int titleId, @StringRes
            int messageId) {
        UsernameRemoveDialogFragment usernameRemoveDialogFragment = new UsernameRemoveDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARG_USERNAME, username);
        args.putInt(ARG_TITLE, titleId);
        args.putInt(ARG_MESSAGE, messageId);

        usernameRemoveDialogFragment.setArguments(args);
        return usernameRemoveDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(getArguments().getInt(ARG_TITLE));
        builder.setMessage(getString(getArguments().getInt(ARG_MESSAGE), getArguments().getString(ARG_USERNAME)));
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (getTargetFragment() instanceof UsernameRemover) {
                    UsernameRemover usernameRemover = (UsernameRemover) getTargetFragment();
                    usernameRemover.removeUsername(getArguments().getString(ARG_USERNAME));
                }
            }
        });
        return builder.create();
    }

    public static <T extends Fragment & UsernameRemover> void show(String username, @StringRes int titleId, @StringRes
            int messageId, T parentFragment, int requestCode) {
        UsernameRemoveDialogFragment usernameRemoveDialogFragment = UsernameRemoveDialogFragment.newInstance
                (username, titleId, messageId);
        usernameRemoveDialogFragment.setTargetFragment(parentFragment, requestCode);
        usernameRemoveDialogFragment.show(parentFragment.getFragmentManager(), null);
    }
}
