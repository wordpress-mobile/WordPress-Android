package org.wordpress.android.ui.people;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Capability;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class PersonDetailFragment extends Fragment {
    private static String ARG_PERSON_ID = "person_id";
    private static String ARG_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";

    private long mPersonID;
    private int mLocalTableBlogID;

    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private LinearLayout mRoleContainer;
    private TextView mRoleTextView;

    private String mSelectedRole;
    private OnChangeListener mListener;

    public static PersonDetailFragment newInstance(long personID, int localTableBlogID) {
        PersonDetailFragment personDetailFragment = new PersonDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_PERSON_ID, personID);
        bundle.putInt(ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        personDetailFragment.setArguments(bundle);
        return personDetailFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnChangeListener");
        }
    }

    // We need to override this for devices pre API 23
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.person_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Sets the enter & pop animation for the fragment. In order to keep the animation even after the configuration
     * changes, this method is used instead of FragmentTransaction for the animation.
     */
    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if (enter) {
            return AnimatorInflater.loadAnimator(getActivity(), R.animator.fragment_slide_in_from_right);
        } else {
            return AnimatorInflater.loadAnimator(getActivity(), R.animator.fragment_slide_out_to_right);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.person_detail_fragment, container, false);

        mPersonID = getArguments().getLong(ARG_PERSON_ID);
        mLocalTableBlogID = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);

        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.person_avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.person_display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.person_username);
        mRoleContainer = (LinearLayout) rootView.findViewById(R.id.person_role_container);
        mRoleTextView = (TextView) rootView.findViewById(R.id.person_role);

        Account account = AccountHelper.getDefaultAccount();
        boolean isCurrentUser = account.getUserId() == mPersonID;
        Blog blog = WordPress.getBlog(mLocalTableBlogID);
        if (!isCurrentUser && blog != null && blog.hasCapability(Capability.REMOVE_USERS)) {
            setHasOptionsMenu(true);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPersonDetails();
    }

    public void refreshPersonDetails() {
        if (!isAdded()) return;

        Person person = getCurrentPerson();
        if (person != null) {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), avatarSz);

            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            mDisplayNameTextView.setText(person.getDisplayName());
            mUsernameTextView.setText(person.getUsername());
            mRoleTextView.setText(StringUtils.capitalize(person.getRole()));

            setupRoleContainerForCapability();
            mSelectedRole = person.getRole();
        } else {
            AppLog.w(AppLog.T.PEOPLE, "Person returned null from DB for personID: " + mPersonID
                    + " & localTableBlogID: " + mLocalTableBlogID);
        }
    }

    public void setPersonDetails(long personID, int localTableBlogID) {
        mPersonID = personID;
        mLocalTableBlogID = localTableBlogID;
        refreshPersonDetails();
    }

    // Checks current user's capabilities to decide whether she can change the role or not
    private void setupRoleContainerForCapability() {
        Blog blog = WordPress.getBlog(mLocalTableBlogID);
        Account account = AccountHelper.getDefaultAccount();
        boolean isCurrentUser = account.getUserId() == mPersonID;
        boolean canChangeRole = (blog != null) && !isCurrentUser && blog.hasCapability(Capability.PROMOTE_USERS);
        if (canChangeRole) {
            mRoleContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRoleChangeDialog();
                }
            });
        } else {
            // Remove the selectableItemBackground if the user can't be edited
            clearRoleContainerBackground();
            // Change transparency to give a visual cue to the user that it's disabled
            mRoleContainer.setAlpha(0.5f);
        }
    }

    @SuppressWarnings("deprecation")
    private void clearRoleContainerBackground() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mRoleContainer.setBackgroundDrawable(null);
        } else {
            mRoleContainer.setBackground(null);
        }
    }

    private void showRoleChangeDialog() {
        Context context = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.role);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Person person = getCurrentPerson();
                if (person != null) {
                    // reset the selected role since the dialog is cancelled
                    mSelectedRole = person.getRole();
                }
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mListener != null) {
                    mListener.onRoleChanged(mPersonID, mLocalTableBlogID, mSelectedRole);
                }
            }
        });

        final String[] roles = getResources().getStringArray(R.array.roles);
        ArrayAdapter<String> roleAdapter = new RoleListAdapter(context, R.layout.role_list_row, roles);
        builder.setAdapter(roleAdapter, null);

        builder.show();
    }

    private class RoleListAdapter extends ArrayAdapter<String> {
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
                radioButton.setChecked(mSelectedRole.equalsIgnoreCase(role));
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
    }

    public Person getCurrentPerson() {
        return PeopleTable.getPerson(mPersonID, mLocalTableBlogID);
    }

    // Container Activity must implement this interface
    public interface OnChangeListener {
        void onRoleChanged(long personID, int localTableBlogId, String newRole);
    }
}
