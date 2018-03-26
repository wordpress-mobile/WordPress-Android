package org.wordpress.android.ui.people;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.RoleUtils;

import java.util.List;

import javax.inject.Inject;

public class RoleSelectDialogFragment extends DialogFragment {
    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SiteModel site = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        final List<RoleModel> inviteRoles = RoleUtils.getInviteRoles(mSiteStore, site, this);
        final String[] stringRoles = new String[inviteRoles.size()];
        for (int i = 0; i < inviteRoles.size(); i++) {
            stringRoles[i] = inviteRoles.get(i).getDisplayName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog));
        builder.setTitle(R.string.role);
        builder.setItems(stringRoles, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!isAdded()) {
                    return;
                }

                if (getTargetFragment() instanceof OnRoleSelectListener) {
                    ((OnRoleSelectListener) getTargetFragment()).onRoleSelected(inviteRoles.get(which));
                } else if (getActivity() instanceof OnRoleSelectListener) {
                    ((OnRoleSelectListener) getActivity()).onRoleSelected(inviteRoles.get(which));
                }
            }
        });

        return builder.create();
    }

    public static <T extends Fragment & OnRoleSelectListener> void show(T parentFragment, int requestCode,
                                                                        @NonNull SiteModel site) {
        RoleSelectDialogFragment roleChangeDialogFragment = new RoleSelectDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        roleChangeDialogFragment.setArguments(args);
        roleChangeDialogFragment.setTargetFragment(parentFragment, requestCode);
        roleChangeDialogFragment.show(parentFragment.getFragmentManager(), null);
    }

    public static <T extends Activity & OnRoleSelectListener> void show(T parentActivity) {
        RoleSelectDialogFragment roleChangeDialogFragment = new RoleSelectDialogFragment();
        roleChangeDialogFragment.show(parentActivity.getFragmentManager(), null);
    }

    // Container Activity must implement this interface
    interface OnRoleSelectListener {
        void onRoleSelected(RoleModel newRole);
    }
}
