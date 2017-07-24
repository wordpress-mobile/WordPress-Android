package org.wordpress.android.ui.people;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;

import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class RoleChangeDialogFragment extends DialogFragment {
    private static final String PERSON_ID_TAG = "person_id";
    private static final String ROLE_TAG = "role";

    @Inject SiteStore mSiteStore;

    private RoleListAdapter mRoleListAdapter;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        RoleModel role = mRoleListAdapter.getSelectedRole();
//        outState.putSerializable(ROLE_TAG, role);
    }

    public static RoleChangeDialogFragment newInstance(long personID, SiteModel site, String role) {
        RoleChangeDialogFragment roleChangeDialogFragment = new RoleChangeDialogFragment();
        Bundle args = new Bundle();

        args.putLong(PERSON_ID_TAG, personID);
//        args.putSerializable(ROLE_TAG, role);
        args.putSerializable(WordPress.SITE, site);

        roleChangeDialogFragment.setArguments(args);
        return roleChangeDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SiteModel site = (SiteModel) getArguments().getSerializable(WordPress.SITE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.role);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RoleModel role = mRoleListAdapter.getSelectedRole();
                Bundle args = getArguments();
                if (args != null) {
                    long personID = args.getLong(PERSON_ID_TAG);
                    if (site != null) {
                        EventBus.getDefault().post(new RoleChangeEvent(personID, site.getId(), role));
                    }
                }
            }
        });

        if (mRoleListAdapter == null && site != null) {
            List<RoleModel> roleList = mSiteStore.getUserRoles(site);
            RoleModel[] userRoles = roleList.toArray(new RoleModel[roleList.size()]);
            mRoleListAdapter = new RoleListAdapter(getActivity(), R.layout.role_list_row, userRoles);
        }
        if (savedInstanceState != null) {
            RoleModel savedRole = (RoleModel) savedInstanceState.getSerializable(ROLE_TAG);
            mRoleListAdapter.setSelectedRole(savedRole);
        } else {
            Bundle args = getArguments();
            if (args != null) {
                RoleModel role = (RoleModel) args.getSerializable(ROLE_TAG);
                mRoleListAdapter.setSelectedRole(role);
            }
        }
        builder.setAdapter(mRoleListAdapter, null);

        return builder.create();
    }

    private class RoleListAdapter extends ArrayAdapter<RoleModel> {
        private RoleModel mSelectedRole;

        RoleListAdapter(Context context, int resource, RoleModel[] userRoles) {
            super(context, resource, userRoles);
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.role_list_row, null);
            }

            TextView mainText = (TextView) convertView.findViewById(R.id.role_label);
            final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeSelection(position);
                    }
                });
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSelection(position);
                }
            });

            RoleModel role = getItem(position);
            if (role != null) {
                radioButton.setChecked(role.equals(mSelectedRole));
                mainText.setText(role.getDisplayName());
            }

            return convertView;
        }

        private void changeSelection(int position) {
            mSelectedRole = getItem(position);
            notifyDataSetChanged();
        }

        RoleModel getSelectedRole() {
            return mSelectedRole;
        }

        void setSelectedRole(RoleModel role) {
            mSelectedRole = role;
        }
    }

    static class RoleChangeEvent {
        final long personID;
        final int localTableBlogId;
        final RoleModel newRole;

        RoleChangeEvent(long personID, int localTableBlogId, RoleModel newRole) {
            this.personID = personID;
            this.localTableBlogId = localTableBlogId;
            this.newRole = newRole;
        }
    }
}
