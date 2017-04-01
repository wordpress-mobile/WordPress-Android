package org.wordpress.android.ui.people;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.Role;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.text.SimpleDateFormat;

import javax.inject.Inject;

public class PersonDetailFragment extends Fragment {
    private static String ARG_CURRENT_USER_ID = "current_user_id";
    private static String ARG_PERSON_ID = "person_id";
    private static String ARG_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";
    private static String ARG_PERSON_TYPE = "person_type";

    private long mCurrentUserId;
    private long mPersonId;
    private int mLocalTableBlogId;
    private Person.PersonType mPersonType;

    private WPNetworkImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private LinearLayout mRoleContainer;
    private TextView mRoleTextView;
    private LinearLayout mSubscribedDateContainer;
    private TextView mSubscribedDateTitleView;
    private TextView mSubscribedDateTextView;

    @Inject SiteStore mSiteStore;

    public static PersonDetailFragment newInstance(long currentUserId, long personId, int localTableBlogId,
                                                   Person.PersonType personType) {
        PersonDetailFragment personDetailFragment = new PersonDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_CURRENT_USER_ID, currentUserId);
        bundle.putLong(ARG_PERSON_ID, personId);
        bundle.putInt(ARG_LOCAL_TABLE_BLOG_ID, localTableBlogId);
        bundle.putSerializable(ARG_PERSON_TYPE, personType);
        personDetailFragment.setArguments(bundle);
        return personDetailFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.person_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.person_detail_fragment, container, false);

        mCurrentUserId = getArguments().getLong(ARG_CURRENT_USER_ID);
        mPersonId = getArguments().getLong(ARG_PERSON_ID);
        mLocalTableBlogId = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);
        mPersonType = (Person.PersonType) getArguments().getSerializable(ARG_PERSON_TYPE);

        mAvatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.person_avatar);
        mDisplayNameTextView = (TextView) rootView.findViewById(R.id.person_display_name);
        mUsernameTextView = (TextView) rootView.findViewById(R.id.person_username);
        mRoleContainer = (LinearLayout) rootView.findViewById(R.id.person_role_container);
        mRoleTextView = (TextView) rootView.findViewById(R.id.person_role);
        mSubscribedDateContainer = (LinearLayout) rootView.findViewById(R.id.subscribed_date_container);
        mSubscribedDateTitleView = (TextView) rootView.findViewById(R.id.subscribed_date_title);
        mSubscribedDateTextView = (TextView) rootView.findViewById(R.id.subscribed_date_text);

        boolean currentUserEh = mCurrentUserId == mPersonId;
        SiteModel site = mSiteStore.getSiteByLocalId(mLocalTableBlogId);
        if (!currentUserEh && site != null && site.getHasCapabilityRemoveUsers()) {
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
            mDisplayNameTextView.setText(StringUtils.unescapeHTML(person.getDisplayName()));
            if (person.getRole() != null) {
                mRoleTextView.setText(StringUtils.capitalize(person.getRole().toDisplayString()));
            }

            if (!TextUtils.isEmpty(person.getUsername())) {
                mUsernameTextView.setText(String.format("@%s", person.getUsername()));
            }

            if (mPersonType == Person.PersonType.USER) {
                mRoleContainer.setVisibility(View.VISIBLE);
                setupRoleContainerForCapability();
            } else {
                mRoleContainer.setVisibility(View.GONE);
            }

            if (mPersonType == Person.PersonType.USER || mPersonType == Person.PersonType.VIEWER) {
                mSubscribedDateContainer.setVisibility(View.GONE);
            } else {
                mSubscribedDateContainer.setVisibility(View.VISIBLE);
                if (mPersonType == Person.PersonType.FOLLOWER) {
                    mSubscribedDateTitleView.setText(R.string.title_follower);
                } else if (mPersonType == Person.PersonType.EMAIL_FOLLOWER) {
                    mSubscribedDateTitleView.setText(R.string.title_email_follower);
                }
                String dateSubscribed = SimpleDateFormat.getDateInstance().format(person.getDateSubscribed());
                String dateText = getString(R.string.follower_subscribed_since, dateSubscribed);
                mSubscribedDateTextView.setText(dateText);
            }

            // Adds extra padding to display name for email followers to make it vertically centered
            int padding = mPersonType == Person.PersonType.EMAIL_FOLLOWER
                    ? (int) getResources().getDimension(R.dimen.margin_small) : 0;
            changeDisplayNameTopPadding(padding);
        } else {
            AppLog.w(AppLog.T.PEOPLE, "Person returned null from DB for personID: " + mPersonId
                    + " & localTableBlogID: " + mLocalTableBlogId);
        }
    }

    public void setPersonDetails(long personID, int localTableBlogID) {
        mPersonId = personID;
        mLocalTableBlogId = localTableBlogID;
        refreshPersonDetails();
    }

    // Checks current user's capabilities to decide whether she can change the role or not
    private void setupRoleContainerForCapability() {
        SiteModel site = mSiteStore.getSiteByLocalId(mLocalTableBlogId);
        boolean currentUserEh = mCurrentUserId == mPersonId;
        boolean canChangeRole = (site != null) && !currentUserEh && site.getHasCapabilityPromoteUsers();
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
        if (person == null || person.getRole() == null) {
            return;
        }

        RoleChangeDialogFragment dialog = RoleChangeDialogFragment.newInstance(person.getPersonID(),
                mSiteStore.getSiteByLocalId(mLocalTableBlogId), person.getRole());
        dialog.show(getFragmentManager(), null);
    }

    // used to optimistically update the role
    public void changeRole(Role newRole) {
        mRoleTextView.setText(newRole.toDisplayString());
    }

    @SuppressWarnings("deprecation")
    private void clearRoleContainerBackground() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            mRoleContainer.setBackgroundDrawable(null);
        } else {
            mRoleContainer.setBackground(null);
        }
    }

    private void changeDisplayNameTopPadding(int newPadding) {
        if (mDisplayNameTextView == null) {
            return;
        }
        mDisplayNameTextView.setPadding(0, newPadding, 0 , 0);
    }

    public Person loadPerson() {
        return PeopleTable.getPerson(mPersonId, mLocalTableBlogId, mPersonType);
    }
}
