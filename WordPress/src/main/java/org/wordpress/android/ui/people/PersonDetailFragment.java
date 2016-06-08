package org.wordpress.android.ui.people;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

    public static PersonDetailFragment newInstance(long personID, int localTableBlogID) {
        PersonDetailFragment personDetailFragment = new PersonDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_PERSON_ID, personID);
        bundle.putInt(ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        personDetailFragment.setArguments(bundle);
        return personDetailFragment;
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

        Person person = loadPerson();
        if (person != null) {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.people_avatar_sz);
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), avatarSz);

            mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
            mDisplayNameTextView.setText(person.getDisplayName());
            mUsernameTextView.setText(person.getUsername());
            mRoleTextView.setText(StringUtils.capitalize(person.getRole()));

            setupRoleContainerForCapability();
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

    private void showRoleChangeDialog() {
        Person person = loadPerson();
        if (person == null) {
            return;
        }

        RoleChangeDialogFragment dialog = RoleChangeDialogFragment.newInstance(person.getPersonID(),
                person.getLocalTableBlogId(), person.getRole());
        dialog.show(getFragmentManager(), null);
    }

    // used to optimistically update the role
    public void changeRole(String newRole) {
        mRoleTextView.setText(newRole);
    }

    @SuppressWarnings("deprecation")
    private void clearRoleContainerBackground() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mRoleContainer.setBackgroundDrawable(null);
        } else {
            mRoleContainer.setBackground(null);
        }
    }

    public Person loadPerson() {
        return PeopleTable.getPerson(mPersonID, mLocalTableBlogID);
    }
}
