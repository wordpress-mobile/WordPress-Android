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

import de.greenrobot.event.EventBus;

public class RoleChangeDialogFragment extends DialogFragment {
    private static final String PERSON_ID_TAG = "person_id";
    private static final String PERSON_LOCAL_TABLE_BLOG_ID_TAG = "local_table_blog_id";
    private static final String ROLE_TAG = "role";

    private RoleListAdapter mRoleListAdapter;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String role = mRoleListAdapter.getSelectedRole();
        outState.putString(ROLE_TAG, role);
    }

    public static RoleChangeDialogFragment newInstance(long personID, int localTableBlogId, String role) {
        RoleChangeDialogFragment roleChangeDialogFragment = new RoleChangeDialogFragment();
        Bundle args = new Bundle();

        args.putLong(PERSON_ID_TAG, personID);
        args.putInt(PERSON_LOCAL_TABLE_BLOG_ID_TAG, localTableBlogId);
        if (role != null) {
            args.putString(ROLE_TAG, role);
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
                String role = mRoleListAdapter.getSelectedRole();
                Bundle args = getArguments();
                if (args != null) {
                    long personID = args.getLong(PERSON_ID_TAG);
                    int localTableBlogId = args.getInt(PERSON_LOCAL_TABLE_BLOG_ID_TAG);
                    EventBus.getDefault().post(new RoleChangeEvent(personID, localTableBlogId, role));
                }
            }
        });

        if (mRoleListAdapter == null) {
            final String[] roles = getResources().getStringArray(R.array.roles);
            mRoleListAdapter = new RoleListAdapter(getActivity(), R.layout.role_list_row, roles);
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

    private class RoleListAdapter extends ArrayAdapter<String> {
        private String mSelectedRole;

        public RoleListAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.role_list_row, null);
            }

            final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            TextView mainText = (TextView) convertView.findViewById(R.id.role_label);
            String role = getItem(position);
            mainText.setText(role);

            if (radioButton != null) {
                radioButton.setChecked(role.equalsIgnoreCase(mSelectedRole));
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

        public String getSelectedRole() {
            return mSelectedRole;
        }

        public void setSelectedRole(String role) {
            mSelectedRole = role;
        }
    }

    public static class RoleChangeEvent {
        public final long personID;
        public final int localTableBlogId;
        public final String newRole;

        public RoleChangeEvent(long personID, int localTableBlogId, String newRole) {
            this.personID = personID;
            this.localTableBlogId = localTableBlogId;
            this.newRole = newRole;
        }
    }
}
