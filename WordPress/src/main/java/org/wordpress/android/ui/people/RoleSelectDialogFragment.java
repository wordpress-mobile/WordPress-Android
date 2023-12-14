package org.wordpress.android.ui.people;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.RoleUtils;
import org.wordpress.android.viewmodel.ContextProvider;

import java.util.List;

import javax.inject.Inject;

public class RoleSelectDialogFragment extends DialogFragment {
    @Inject SiteStore mSiteStore;
    @Inject ContextProvider mContextProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) requireActivity().getApplicationContext()).component().inject(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SiteModel site = (SiteModel) requireArguments().getSerializable(WordPress.SITE);
        final List<RoleModel> inviteRoles = RoleUtils.getInviteRoles(mSiteStore, site, mContextProvider.getContext());
        final String[] stringRoles = new String[inviteRoles.size()];
        for (int i = 0; i < inviteRoles.size(); i++) {
            stringRoles[i] = inviteRoles.get(i).getDisplayName();
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireActivity());
        builder.setTitle(R.string.role);
        builder.setItems(stringRoles, (dialog, which) -> {
            if (!isAdded()) {
                return;
            }

            if (getParentFragment() instanceof OnRoleSelectListener) {
                ((OnRoleSelectListener) getParentFragment()).onRoleSelected(inviteRoles.get(which));
            } else if (getActivity() instanceof OnRoleSelectListener) {
                ((OnRoleSelectListener) getActivity()).onRoleSelected(inviteRoles.get(which));
            }
        });

        return builder.create();
    }

    public static <T extends Fragment & OnRoleSelectListener> void show(
            @NonNull T parentFragment,
            @NonNull SiteModel site
    ) {
        RoleSelectDialogFragment roleChangeDialogFragment = new RoleSelectDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        roleChangeDialogFragment.setArguments(args);
        roleChangeDialogFragment.show(parentFragment.getChildFragmentManager(), null);
    }

    // Container Activity must implement this interface
    interface OnRoleSelectListener {
        void onRoleSelected(RoleModel newRole);
    }
}
