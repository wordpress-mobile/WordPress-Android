package org.wordpress.android.ui.people;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;

import org.wordpress.android.R;

public class RoleSelectDialogFragment extends DialogFragment {
    private static final String IS_PRIVATE_TAG = "is_private";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean isPrivateSite = getArguments().getBoolean(IS_PRIVATE_TAG);
        int roleRes = isPrivateSite ? R.array.invite_roles_private : R.array.invite_roles_public;
        final String[] roles = getResources().getStringArray(roleRes);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.role);
        builder.setItems(roles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!isAdded()) {
                    return;
                }

                if (getTargetFragment() instanceof OnRoleSelectListener) {
                    ((OnRoleSelectListener) getTargetFragment()).onRoleSelected(roles[which]);
                } else if (getActivity() instanceof OnRoleSelectListener) {
                    ((OnRoleSelectListener) getActivity()).onRoleSelected(roles[which]);
                }
            }
        });

        return builder.create();
    }

    public static <T extends Fragment & OnRoleSelectListener> void show(T parentFragment, int requestCode,
                                                                        boolean isPrivateSite) {
        RoleSelectDialogFragment roleChangeDialogFragment = new RoleSelectDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(IS_PRIVATE_TAG, isPrivateSite);
        roleChangeDialogFragment.setArguments(args);
        roleChangeDialogFragment.setTargetFragment(parentFragment, requestCode);
        roleChangeDialogFragment.show(parentFragment.getFragmentManager(), null);
    }

    public static <T extends Activity & OnRoleSelectListener> void show(T parentActivity) {
        RoleSelectDialogFragment roleChangeDialogFragment = new RoleSelectDialogFragment();
        roleChangeDialogFragment.show(parentActivity.getFragmentManager(), null);
    }

    // Container Activity must implement this interface
    public interface OnRoleSelectListener {
        void onRoleSelected(String newRole);
    }
}
