package org.wordpress.android.ui.people;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.List;

import javax.inject.Inject;

public class RoleChangeDialogFragment extends DialogFragment {
    private static final String PERSON_ID_TAG = "person_id";
    private static final String ROLE_TAG = "role";

    @Inject SiteStore mSiteStore;

    private RoleListAdapter mRoleListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String role = mRoleListAdapter.getSelectedRole();
        outState.putSerializable(ROLE_TAG, role);
    }

    public static RoleChangeDialogFragment newInstance(long personID, SiteModel site, String role) {
        RoleChangeDialogFragment roleChangeDialogFragment = new RoleChangeDialogFragment();
        Bundle args = new Bundle();

        args.putLong(PERSON_ID_TAG, personID);
        args.putString(ROLE_TAG, role);
        args.putSerializable(WordPress.SITE, site);

        roleChangeDialogFragment.setArguments(args);
        return roleChangeDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SiteModel site = (SiteModel) getArguments().getSerializable(WordPress.SITE);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.role);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String role = mRoleListAdapter.getSelectedRole();
            Bundle args = getArguments();
            if (args != null) {
                long personID = args.getLong(PERSON_ID_TAG);
                if (site != null) {
                    EventBus.getDefault().post(new RoleChangeEvent(personID, site.getId(), role));
                }
            }
        });

        if (mRoleListAdapter == null && site != null) {
            List<RoleModel> roleList = mSiteStore.getUserRoles(site);
            RoleModel[] userRoles = roleList.toArray(new RoleModel[roleList.size()]);
            mRoleListAdapter = new RoleListAdapter(getActivity(), R.layout.role_list_row, userRoles);
        }
        if (savedInstanceState != null) {
            String savedRole = savedInstanceState.getString(ROLE_TAG);
            mRoleListAdapter.setSelectedRole(savedRole);
        } else {
            Bundle args = getArguments();
            if (args != null) {
                String role = args.getString(ROLE_TAG);
                mRoleListAdapter.setSelectedRole(role);
            }
        }
        builder.setAdapter(mRoleListAdapter, null);

        return builder.create();
    }

    private class RoleListAdapter extends ArrayAdapter<RoleModel> {
        private String mSelectedRole;

        RoleListAdapter(Context context, int resource, RoleModel[] userRoles) {
            super(context, resource, userRoles);
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.role_list_row, null);
            }

            TextView mainText = convertView.findViewById(R.id.role_label);
            final RadioButton radioButton = convertView.findViewById(R.id.radio);
            radioButton.setOnClickListener(v -> changeSelection(position));
            convertView.setOnClickListener(v -> changeSelection(position));

            RoleModel role = getItem(position);
            if (role != null) {
                radioButton.setChecked(role.getName().equals(mSelectedRole));
                mainText.setText(role.getDisplayName());
            }

            return convertView;
        }

        private void changeSelection(int position) {
            RoleModel roleModel = getItem(position);
            if (roleModel != null) {
                mSelectedRole = roleModel.getName();
                notifyDataSetChanged();
            }
        }

        String getSelectedRole() {
            return mSelectedRole;
        }

        void setSelectedRole(String role) {
            mSelectedRole = role;
        }
    }

    static class RoleChangeEvent {
        private final long mPersonID;
        private final int mLocalTableBlogId;
        private final String mNewRole;

        RoleChangeEvent(long personID, int localTableBlogId, String newRole) {
            mPersonID = personID;
            mLocalTableBlogId = localTableBlogId;
            mNewRole = newRole;
        }

        long getPersonID() {
            return mPersonID;
        }

        int getLocalTableBlogId() {
            return mLocalTableBlogId;
        }

        String getNewRole() {
            return mNewRole;
        }
    }
}
