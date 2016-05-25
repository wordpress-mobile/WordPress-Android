package org.wordpress.android.ui.people;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;

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

    protected static RoleChangeDialogFragment newInstance(long personID, int localTableBlogId, String role) {
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
                if (!isAdded()) {
                    return;
                }

                if (getTargetFragment() instanceof OnChangeListener) {
                    launchListener(((OnChangeListener) getTargetFragment()));
                } else if (getActivity() instanceof OnChangeListener) {
                    launchListener(((OnChangeListener) getActivity()));
                }
            }

            private void launchListener(OnChangeListener onChangeListener) {
                String role = mRoleListAdapter.getSelectedRole();
                Bundle args = getArguments();
                if (args != null) {
                    long personID = args.getLong(PERSON_ID_TAG);
                    int localTableBlogId = args.getInt(PERSON_LOCAL_TABLE_BLOG_ID_TAG);
                    onChangeListener.onRoleChanged(personID, localTableBlogId, role);
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

    public static <T extends Fragment & OnChangeListener> void show(T parentFragment, long personID, int
            localTableBlogId, String role, int requestCode) {
        RoleChangeDialogFragment roleChangeDialogFragment = RoleChangeDialogFragment.newInstance(personID,
                localTableBlogId, role);
        roleChangeDialogFragment.setTargetFragment(parentFragment, requestCode);
        roleChangeDialogFragment.show(parentFragment.getFragmentManager(), null);
    }

    public static <T extends Activity & OnChangeListener> void show(T parentActivity, long personID, int
            localTableBlogId, String role) {
        RoleChangeDialogFragment roleChangeDialogFragment = RoleChangeDialogFragment.newInstance(personID,
                localTableBlogId, role);
        roleChangeDialogFragment.show(parentActivity.getFragmentManager(), null);
    }

    // Container Activity must implement this interface
    public interface OnChangeListener {
        void onRoleChanged(long personID, int localTableBlogId, String newRole);
    }
}
