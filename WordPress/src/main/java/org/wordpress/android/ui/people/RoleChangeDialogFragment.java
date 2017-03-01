package org.wordpress.android.ui.people;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Role;

import de.greenrobot.event.EventBus;

public class RoleChangeDialogFragment extends DialogFragment {
    private static final String PERSON_ID_TAG = "person_id";
    private static final String PERSON_LOCAL_TABLE_BLOG_ID_TAG = "local_table_blog_id";
    private static final String ROLE_TAG = "role";

    private RoleListAdapter mRoleListAdapter;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Role role = mRoleListAdapter.getSelectedRole();
        outState.putSerializable(ROLE_TAG, role);
    }

    public static RoleChangeDialogFragment newInstance(long personID, int localTableBlogId, Role role) {
        RoleChangeDialogFragment roleChangeDialogFragment = new RoleChangeDialogFragment();
        Bundle args = new Bundle();

        args.putLong(PERSON_ID_TAG, personID);
        args.putInt(PERSON_LOCAL_TABLE_BLOG_ID_TAG, localTableBlogId);
        if (role != null) {
            args.putSerializable(ROLE_TAG, role);
        }

        roleChangeDialogFragment.setArguments(args);
        return roleChangeDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.role);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Role role = mRoleListAdapter.getSelectedRole();
                Bundle args = getArguments();
                if (args != null) {
                    long personID = args.getLong(PERSON_ID_TAG);
                    int localTableBlogId = args.getInt(PERSON_LOCAL_TABLE_BLOG_ID_TAG);
                    EventBus.getDefault().post(new RoleChangeEvent(personID, localTableBlogId, role));
                }
            }
        });

        if (mRoleListAdapter == null) {
            final Role[] userRoles = Role.userRoles();
            mRoleListAdapter = new RoleListAdapter(getActivity(), R.layout.role_list_row, userRoles);
        }
        if (savedInstanceState != null) {
            Role savedRole = (Role) savedInstanceState.getSerializable(ROLE_TAG);
            mRoleListAdapter.setSelectedRole(savedRole);
        } else {
            Bundle args = getArguments();
            if (args != null) {
                Role role = (Role) args.getSerializable(ROLE_TAG);
                mRoleListAdapter.setSelectedRole(role);
            }
        }
        builder.setAdapter(mRoleListAdapter, null);

        return builder.create();
    }

    private class RoleListAdapter extends ArrayAdapter<Role> {
        private Role mSelectedRole;

        public RoleListAdapter(Context context, int resource, Role[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.role_list_row, null);
            }

            final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            TextView mainText = (TextView) convertView.findViewById(R.id.role_label);
            Role role = getItem(position);
            mainText.setText(role.toDisplayString());

            if (radioButton != null) {
                radioButton.setChecked(role == mSelectedRole);
                radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeSelection(position);
                    }
                });
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSelection(position);
                }
            });

            return convertView;
        }

        private void changeSelection(int position) {
            mSelectedRole = getItem(position);
            notifyDataSetChanged();
        }

        public Role getSelectedRole() {
            return mSelectedRole;
        }

        public void setSelectedRole(Role role) {
            mSelectedRole = role;
        }
    }

    public static class RoleChangeEvent {
        public final long personID;
        public final int localTableBlogId;
        public final Role newRole;

        public RoleChangeEvent(long personID, int localTableBlogId, Role newRole) {
            this.personID = personID;
            this.localTableBlogId = localTableBlogId;
            this.newRole = newRole;
        }
    }
}
