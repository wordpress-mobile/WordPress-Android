package org.wordpress.android.ui.people;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.RoleUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.inject.Inject;

public class PersonDetailFragment extends Fragment {
    private static final String ARG_CURRENT_USER_ID = "current_user_id";
    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";
    private static final String ARG_PERSON_TYPE = "person_type";

    private long mCurrentUserId;
    private long mPersonId;
    private int mLocalTableBlogId;
    private Person.PersonType mPersonType;

    private List<RoleModel> mUserRoles;

    private ImageView mAvatarImageView;
    private TextView mDisplayNameTextView;
    private TextView mUsernameTextView;
    private LinearLayout mRoleContainer;
    private TextView mRoleTextView;
    private LinearLayout mSubscribedDateContainer;
    private TextView mSubscribedDateTitleView;
    private TextView mSubscribedDateTextView;

    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;

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

        if (savedInstanceState == null) {
            mCurrentUserId = getArguments().getLong(ARG_CURRENT_USER_ID);
            mPersonId = getArguments().getLong(ARG_PERSON_ID);
            mLocalTableBlogId = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);
            mPersonType = (Person.PersonType) getArguments().getSerializable(ARG_PERSON_TYPE);
        } else {
            mCurrentUserId = savedInstanceState.getLong(ARG_CURRENT_USER_ID);
            mPersonId = savedInstanceState.getLong(ARG_PERSON_ID);
            mLocalTableBlogId = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
            mPersonType = (Person.PersonType) savedInstanceState.getSerializable(ARG_PERSON_TYPE);
        }

        SiteModel siteModel = mSiteStore.getSiteByLocalId(mLocalTableBlogId);
        mUserRoles = mSiteStore.getUserRoles(siteModel);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARG_CURRENT_USER_ID, mCurrentUserId);
        outState.putLong(ARG_PERSON_ID, mPersonId);
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalTableBlogId);
        outState.putSerializable(ARG_PERSON_TYPE, mPersonType);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.person_detail, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.person_detail_fragment, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar_main);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(null);
        }

        mAvatarImageView = rootView.findViewById(R.id.person_avatar);
        mDisplayNameTextView = rootView.findViewById(R.id.person_display_name);
        mUsernameTextView = rootView.findViewById(R.id.person_username);
        mRoleContainer = rootView.findViewById(R.id.person_role_container);
        mRoleTextView = rootView.findViewById(R.id.person_role);
        mSubscribedDateContainer = rootView.findViewById(R.id.subscribed_date_container);
        mSubscribedDateTitleView = rootView.findViewById(R.id.subscribed_date_title);
        mSubscribedDateTextView = rootView.findViewById(R.id.subscribed_date_text);

        boolean isCurrentUser = mCurrentUserId == mPersonId;
        SiteModel site = mSiteStore.getSiteByLocalId(mLocalTableBlogId);
        if (!isCurrentUser && site != null && site.getHasCapabilityRemoveUsers()) {
            setHasOptionsMenu(true);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPersonDetails();
    }

    void refreshPersonDetails() {
        if (!isAdded()) {
            return;
        }

        Person person = loadPerson();
        if (person != null) {
            int avatarSz = getResources().getDimensionPixelSize(R.dimen.people_avatar_sz);
            String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), avatarSz);

            mImageManager.loadIntoCircle(mAvatarImageView, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);
            mDisplayNameTextView.setText(StringEscapeUtils.unescapeHtml4(person.getDisplayName()));
            if (person.getRole() != null) {
                mRoleTextView.setText(RoleUtils.getDisplayName(person.getRole(), mUserRoles));
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

    void setPersonDetails(long personID, int localTableBlogID) {
        mPersonId = personID;
        mLocalTableBlogId = localTableBlogID;
        refreshPersonDetails();
    }

    // Checks current user's capabilities to decide whether she can change the role or not
    private void setupRoleContainerForCapability() {
        SiteModel site = mSiteStore.getSiteByLocalId(mLocalTableBlogId);
        boolean isCurrentUser = mCurrentUserId == mPersonId;
        boolean canChangeRole = (site != null) && !isCurrentUser && site.getHasCapabilityPromoteUsers();
        if (canChangeRole) {
            mRoleContainer.setOnClickListener(v -> showRoleChangeDialog());
        } else {
            // Remove the selectableItemBackground if the user can't be edited
            mRoleContainer.setBackground(null);
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
                mSiteStore.getSiteByLocalId(
                        mLocalTableBlogId),
                person.getRole());
        dialog.show(getFragmentManager(), null);
    }

    // used to optimistically update the role
    void changeRole(String newRole) {
        mRoleTextView.setText(RoleUtils.getDisplayName(newRole, mUserRoles));
    }

    private void changeDisplayNameTopPadding(int newPadding) {
        if (mDisplayNameTextView == null) {
            return;
        }
        mDisplayNameTextView.setPadding(0, newPadding, 0, 0);
    }

    Person loadPerson() {
        return PeopleTable.getPerson(mPersonId, mLocalTableBlogId, mPersonType);
    }
}
