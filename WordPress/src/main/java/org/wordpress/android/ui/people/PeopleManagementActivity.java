package org.wordpress.android.ui.people;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;

import de.greenrobot.event.EventBus;


public class PeopleManagementActivity extends AppCompatActivity
        implements PeopleListFragment.OnPersonSelectedListener, PeopleListFragment.OnFetchPeopleListener {
    private static final String KEY_PEOPLE_LIST_FRAGMENT = "people-list-fragment";
    private static final String KEY_PERSON_DETAIL_FRAGMENT = "person-detail-fragment";
    private static final String KEY_PEOPLE_INVITE_FRAGMENT = "people-invite-fragment";
    private static final String KEY_TITLE = "page-title";

    private static final String KEY_USERS_END_OF_LIST_REACHED = "users-end-of-list-reached";
    private static final String KEY_FOLLOWERS_END_OF_LIST_REACHED = "followers-end-of-list-reached";
    private static final String KEY_EMAIL_FOLLOWERS_END_OF_LIST_REACHED = "email-followers-end-of-list-reached";
    private static final String KEY_VIEWERS_END_OF_LIST_REACHED = "viewers-end-of-list-reached";

    private static final String KEY_USERS_FETCH_REQUEST_IN_PROGRESS = "users-fetch-request-in-progress";
    private static final String KEY_FOLLOWERS_FETCH_REQUEST_IN_PROGRESS = "followers-fetch-request-in-progress";
    private static final String KEY_EMAIL_FOLLOWERS_FETCH_REQUEST_IN_PROGRESS = "email-followers-fetch-request-in-progress";
    private static final String KEY_VIEWERS_FETCH_REQUEST_IN_PROGRESS = "viewers-fetch-request-in-progress";

    private static final String KEY_HAS_REFRESHED_USERS = "has-refreshed-users";
    private static final String KEY_HAS_REFRESHED_FOLLOWERS = "has-refreshed-followers";
    private static final String KEY_HAS_REFRESHED_EMAIL_FOLLOWERS = "has-refreshed-email-followers";
    private static final String KEY_HAS_REFRESHED_VIEWERS = "has-refreshed-viewers";

    private static final String KEY_FOLLOWERS_LAST_FETCHED_PAGE = "followers-last-fetched-page";
    private static final String KEY_EMAIL_FOLLOWERS_LAST_FETCHED_PAGE = "email-followers-last-fetched-page";

    // End of list reached variables will be true when there is no more data to fetch
    private boolean mUsersEndOfListReached;
    private boolean mFollowersEndOfListReached;
    private boolean mEmailFollowersEndOfListReached;
    private boolean mViewersEndOfListReached;

    // We only allow the lists to be refreshed once to avoid syncing and jumping animation issues
    private boolean mHasRefreshedUsers;
    private boolean mHasRefreshedFollowers;
    private boolean mHasRefreshedEmailFollowers;
    private boolean mHasRefreshedViewers;

    // If we are currently making a request for a certain filter
    private boolean mUsersFetchRequestInProgress;
    private boolean mFollowersFetchRequestInProgress;
    private boolean mEmailFollowersFetchRequestInProgress;
    private boolean mViewersFetchRequestInProgress;

    // Keep track of the last page we received from remote
    private int mFollowersLastFetchedPage;
    private int mEmailFollowersLastFetchedPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.people_management_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }

        Blog blog = WordPress.getCurrentBlog();
        if (blog == null) {
            ToastUtils.showToast(this, R.string.blog_not_found);
            finish();
            return;
        }

        FragmentManager fragmentManager = getFragmentManager();

        if (savedInstanceState == null) {
            // only delete cached people if there is a connection
            if (NetworkUtils.isNetworkAvailable(this)) {
                PeopleTable.deletePeopleExceptForFirstPage(blog.getLocalTableBlogId());
            }

            if (actionBar != null) {
                actionBar.setTitle(R.string.people);
            }

            PeopleListFragment peopleListFragment = PeopleListFragment.newInstance(blog.getLocalTableBlogId());
            peopleListFragment.setOnPersonSelectedListener(this);
            peopleListFragment.setOnFetchPeopleListener(this);

            mUsersEndOfListReached = false;
            mFollowersEndOfListReached = false;
            mEmailFollowersEndOfListReached = false;
            mViewersEndOfListReached = false;

            mHasRefreshedUsers = false;
            mHasRefreshedFollowers = false;
            mHasRefreshedEmailFollowers = false;
            mHasRefreshedViewers = false;

            mUsersFetchRequestInProgress = false;
            mFollowersFetchRequestInProgress = false;
            mEmailFollowersFetchRequestInProgress = false;
            mViewersFetchRequestInProgress = false;
            mFollowersLastFetchedPage = 0;
            mEmailFollowersLastFetchedPage = 0;


            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, peopleListFragment, KEY_PEOPLE_LIST_FRAGMENT)
                    .commit();
        } else {
            mUsersEndOfListReached = savedInstanceState.getBoolean(KEY_USERS_END_OF_LIST_REACHED);
            mFollowersEndOfListReached = savedInstanceState.getBoolean(KEY_FOLLOWERS_END_OF_LIST_REACHED);
            mEmailFollowersEndOfListReached = savedInstanceState.getBoolean(KEY_EMAIL_FOLLOWERS_END_OF_LIST_REACHED);
            mViewersEndOfListReached = savedInstanceState.getBoolean(KEY_VIEWERS_END_OF_LIST_REACHED);

            mHasRefreshedUsers = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_USERS);
            mHasRefreshedFollowers = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_FOLLOWERS);
            mHasRefreshedEmailFollowers = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_EMAIL_FOLLOWERS);
            mHasRefreshedViewers = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_VIEWERS);

            mUsersFetchRequestInProgress = savedInstanceState.getBoolean(KEY_USERS_FETCH_REQUEST_IN_PROGRESS);
            mFollowersFetchRequestInProgress = savedInstanceState.getBoolean(KEY_FOLLOWERS_FETCH_REQUEST_IN_PROGRESS);
            mEmailFollowersFetchRequestInProgress = savedInstanceState.getBoolean(KEY_EMAIL_FOLLOWERS_FETCH_REQUEST_IN_PROGRESS);
            mViewersFetchRequestInProgress = savedInstanceState.getBoolean(KEY_VIEWERS_FETCH_REQUEST_IN_PROGRESS);

            mFollowersLastFetchedPage = savedInstanceState.getInt(KEY_FOLLOWERS_LAST_FETCHED_PAGE);
            mEmailFollowersLastFetchedPage = savedInstanceState.getInt(KEY_EMAIL_FOLLOWERS_LAST_FETCHED_PAGE);

            CharSequence title = savedInstanceState.getCharSequence(KEY_TITLE);
            if (actionBar != null && title != null) {
                actionBar.setTitle(title);
            }

            PeopleListFragment peopleListFragment = getListFragment();
            if (peopleListFragment != null) {
                peopleListFragment.setOnPersonSelectedListener(this);
                peopleListFragment.setOnFetchPeopleListener(this);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_USERS_END_OF_LIST_REACHED, mUsersEndOfListReached);
        outState.putBoolean(KEY_FOLLOWERS_END_OF_LIST_REACHED, mFollowersEndOfListReached);
        outState.putBoolean(KEY_EMAIL_FOLLOWERS_END_OF_LIST_REACHED, mEmailFollowersEndOfListReached);
        outState.putBoolean(KEY_VIEWERS_END_OF_LIST_REACHED, mViewersEndOfListReached);

        outState.putBoolean(KEY_HAS_REFRESHED_USERS, mHasRefreshedUsers);
        outState.putBoolean(KEY_HAS_REFRESHED_FOLLOWERS, mHasRefreshedFollowers);
        outState.putBoolean(KEY_HAS_REFRESHED_EMAIL_FOLLOWERS, mHasRefreshedEmailFollowers);
        outState.putBoolean(KEY_HAS_REFRESHED_VIEWERS, mHasRefreshedViewers);

        outState.putBoolean(KEY_USERS_FETCH_REQUEST_IN_PROGRESS, mUsersFetchRequestInProgress);
        outState.putBoolean(KEY_FOLLOWERS_FETCH_REQUEST_IN_PROGRESS, mFollowersFetchRequestInProgress);
        outState.putBoolean(KEY_EMAIL_FOLLOWERS_FETCH_REQUEST_IN_PROGRESS, mEmailFollowersFetchRequestInProgress);
        outState.putBoolean(KEY_VIEWERS_FETCH_REQUEST_IN_PROGRESS, mViewersFetchRequestInProgress);

        outState.putInt(KEY_FOLLOWERS_LAST_FETCHED_PAGE, mFollowersLastFetchedPage);
        outState.putInt(KEY_EMAIL_FOLLOWERS_LAST_FETCHED_PAGE, mEmailFollowersLastFetchedPage);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            outState.putCharSequence(KEY_TITLE, actionBar.getTitle());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (!navigateBackToPeopleListFragment()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.remove_person) {
            confirmRemovePerson();
            return true;
        } else if (item.getItemId() == R.id.invite) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment peopleInviteFragment = fragmentManager.findFragmentByTag(KEY_PERSON_DETAIL_FRAGMENT);

            if (peopleInviteFragment == null) {
                Blog blog = WordPress.getCurrentBlog();
                String dotComBlogId = blog.getDotComBlogId();
                if (!TextUtils.isEmpty(dotComBlogId)) {
                    peopleInviteFragment = PeopleInviteFragment.newInstance(dotComBlogId);
                } else {
                    String blogUrl = blog.getUrl();
                    AppLog.e(T.PEOPLE, "getDotComBlogId() returned null or empty string! Blog URL: " +
                            (blogUrl == null ? "null" : blogUrl));
                    ToastUtils.showToast(this, R.string.error_generic).show();
                }
            }
            if (peopleInviteFragment != null && !peopleInviteFragment.isAdded()) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, peopleInviteFragment, KEY_PEOPLE_INVITE_FRAGMENT);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        } else if (item.getItemId() == R.id.send_invitation) {
            FragmentManager fragmentManager = getFragmentManager();
            Fragment peopleInviteFragment = fragmentManager.findFragmentByTag(KEY_PEOPLE_INVITE_FRAGMENT);
            if (peopleInviteFragment != null) {
                ((InvitationSender) peopleInviteFragment).send();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean fetchUsersList(String dotComBlogId, final int localTableBlogId, final int offset) {
        if (mUsersEndOfListReached || mUsersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mUsersFetchRequestInProgress = true;

        PeopleUtils.fetchUsers(dotComBlogId, localTableBlogId, offset, new PeopleUtils.FetchUsersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, boolean isEndOfList) {
                boolean isFreshList = (offset == 0);
                mHasRefreshedUsers = true;
                mUsersEndOfListReached = isEndOfList;
                PeopleTable.saveUsers(peopleList, localTableBlogId, isFreshList);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.TEAM, isFreshList, true);
                }

                refreshOnScreenFragmentDetails();
                mUsersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean isFirstPage = offset == 0;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.TEAM, isFirstPage, false);
                }
                mUsersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_users_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    private boolean fetchFollowersList(String dotComBlogId, final int localTableBlogId, final int page) {
        if (mFollowersEndOfListReached || mFollowersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mFollowersFetchRequestInProgress = true;

        PeopleUtils.fetchFollowers(dotComBlogId, localTableBlogId, page, new PeopleUtils.FetchFollowersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, int pageFetched, boolean isEndOfList) {
                boolean isFreshList = (page == 1);
                mHasRefreshedFollowers = true;
                mFollowersLastFetchedPage = pageFetched;
                mFollowersEndOfListReached = isEndOfList;
                PeopleTable.saveFollowers(peopleList, localTableBlogId, isFreshList);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.FOLLOWERS, isFreshList, true);
                }

                refreshOnScreenFragmentDetails();
                mFollowersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean isFirstPage = page == 1;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.FOLLOWERS, isFirstPage, false);
                }
                mFollowersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_followers_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    private boolean fetchEmailFollowersList(String dotComBlogId, final int localTableBlogId, final int page) {
        if (mEmailFollowersEndOfListReached || mEmailFollowersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mEmailFollowersFetchRequestInProgress = true;

        PeopleUtils.fetchEmailFollowers(dotComBlogId, localTableBlogId, page, new PeopleUtils.FetchFollowersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, int pageFetched, boolean isEndOfList) {
                boolean isFreshList = (page == 1);
                mHasRefreshedEmailFollowers = true;
                mEmailFollowersLastFetchedPage = pageFetched;
                mEmailFollowersEndOfListReached = isEndOfList;
                PeopleTable.saveEmailFollowers(peopleList, localTableBlogId, isFreshList);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.EMAIL_FOLLOWERS, isFreshList, true);
                }

                refreshOnScreenFragmentDetails();
                mEmailFollowersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean isFirstPage = page == 1;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.EMAIL_FOLLOWERS, isFirstPage, false);
                }
                mEmailFollowersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_email_followers_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    private boolean fetchViewersList(String dotComBlogId, final int localTableBlogId, final int offset) {
        if (mViewersEndOfListReached || mViewersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mViewersFetchRequestInProgress = true;

        PeopleUtils.fetchViewers(dotComBlogId, localTableBlogId, offset, new PeopleUtils.FetchViewersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, boolean isEndOfList) {
                boolean isFreshList = (offset == 0);
                mHasRefreshedViewers = true;
                mViewersEndOfListReached = isEndOfList;
                PeopleTable.saveViewers(peopleList, localTableBlogId, isFreshList);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.VIEWERS, isFreshList, true);
                }

                refreshOnScreenFragmentDetails();
                mViewersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean isFirstPage = offset == 0;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.VIEWERS, isFirstPage, false);
                }
                mViewersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_viewers_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    @Override
    public void onPersonSelected(Person person) {
        PersonDetailFragment personDetailFragment = getDetailFragment();

        long personID = person.getPersonID();
        int localTableBlogID = person.getLocalTableBlogId();

        if (personDetailFragment == null) {
            personDetailFragment = PersonDetailFragment.newInstance(personID, localTableBlogID, person.getPersonType());
        } else {
            personDetailFragment.setPersonDetails(personID, localTableBlogID);
        }
        if (!personDetailFragment.isAdded()) {
            AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.OPENED_PERSON);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, personDetailFragment, KEY_PERSON_DETAIL_FRAGMENT);
            fragmentTransaction.addToBackStack(null);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle("");
            }

            fragmentTransaction.commit();
        }
    }

    public void onEventMainThread(RoleChangeDialogFragment.RoleChangeEvent event) {
        if(!NetworkUtils.checkConnection(this)) {
            return;
        }

        final Person person = PeopleTable.getUser(event.personID, event.localTableBlogId);
        if (person == null || event.newRole == null || person.getRole() == event.newRole) {
            return;
        }

        String blogId = WordPress.getCurrentRemoteBlogId();
        if (blogId == null) {
            return;
        }

        final PersonDetailFragment personDetailFragment = getDetailFragment();
        if (personDetailFragment != null) {
            // optimistically update the role
            personDetailFragment.changeRole(event.newRole);
        }

        PeopleUtils.updateRole(blogId, person.getPersonID(), event.newRole, event.localTableBlogId,
                new PeopleUtils.UpdateUserCallback() {
            @Override
            public void onSuccess(Person person) {
                AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.PERSON_UPDATED);
                PeopleTable.saveUser(person);
                refreshOnScreenFragmentDetails();
            }

            @Override
            public void onError() {
                // change the role back to it's original value
                if (personDetailFragment != null) {
                    personDetailFragment.refreshPersonDetails();
                }
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_update_role,
                        ToastUtils.Duration.LONG);
            }
        });
    }

    private void confirmRemovePerson() {
        Person person = getCurrentPerson();
        if (person == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Calypso_AlertDialog);
        builder.setTitle(getString(R.string.person_remove_confirmation_title, person.getDisplayName()));
        if (person.getPersonType() == Person.PersonType.USER) {
            builder.setMessage(getString(R.string.user_remove_confirmation_message, person.getDisplayName()));
        } else if(person.getPersonType() == Person.PersonType.VIEWER) {
            builder.setMessage(R.string.viewer_remove_confirmation_message);
        } else {
            builder.setMessage(R.string.follower_remove_confirmation_message);
        }
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeSelectedPerson();
            }
        });
        builder.show();
    }

    private void removeSelectedPerson() {
        if(!NetworkUtils.checkConnection(this)) {
            return;
        }

        Person person = getCurrentPerson();
        if (person == null) {
            return;
        }
        String blogId = WordPress.getCurrentRemoteBlogId();
        if (blogId == null) {
            return;
        }

        final Person.PersonType personType = person.getPersonType();
        final String displayName = person.getDisplayName();

        PeopleUtils.RemovePersonCallback callback = new PeopleUtils.RemovePersonCallback() {
            @Override
            public void onSuccess(long personID, int localTableBlogId) {
                if (personType == Person.PersonType.USER) {
                    AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.PERSON_REMOVED);
                }

                // remove the person from db, navigate back to list fragment and refresh it
                PeopleTable.deletePerson(personID, localTableBlogId, personType);

                String message = getString(R.string.person_removed, displayName);
                ToastUtils.showToast(PeopleManagementActivity.this, message, ToastUtils.Duration.LONG);

                navigateBackToPeopleListFragment();
                refreshPeopleListFragment();
            }

            @Override
            public void onError() {
                int errorMessageRes;
                switch (personType) {
                    case USER:
                        errorMessageRes = R.string.error_remove_user;
                        break;
                    case VIEWER:
                        errorMessageRes = R.string.error_remove_viewer;
                        break;
                    default:
                        errorMessageRes = R.string.error_remove_follower;
                        break;
                }
                ToastUtils.showToast(PeopleManagementActivity.this,
                        errorMessageRes,
                        ToastUtils.Duration.LONG);
            }
        };

        if (personType == Person.PersonType.FOLLOWER || personType == Person.PersonType.EMAIL_FOLLOWER) {
            PeopleUtils.removeFollower(blogId, person.getPersonID(), person.getLocalTableBlogId(),
                    personType, callback);
        } else if(personType == Person.PersonType.VIEWER) {
            PeopleUtils.removeViewer(blogId, person.getPersonID(), person.getLocalTableBlogId(), callback);
        } else {
            PeopleUtils.removeUser(blogId, person.getPersonID(), person.getLocalTableBlogId(), callback);
        }
    }

    // This helper method is used after a successful network request
    private void refreshOnScreenFragmentDetails() {
        refreshPeopleListFragment();
        refreshDetailFragment();
    }

    private void refreshPeopleListFragment() {
        PeopleListFragment peopleListFragment = getListFragment();
        if (peopleListFragment != null) {
            peopleListFragment.refreshPeopleList(false);
        }
    }

    private void refreshDetailFragment() {
        PersonDetailFragment personDetailFragment = getDetailFragment();
        if (personDetailFragment != null) {
            personDetailFragment.refreshPersonDetails();
        }
    }

    private boolean navigateBackToPeopleListFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(R.string.people);
            }
            return true;
        }
        return false;
    }

    private Person getCurrentPerson() {
        PersonDetailFragment personDetailFragment = getDetailFragment();

        if (personDetailFragment == null) {
            return null;
        }

        return personDetailFragment.loadPerson();
    }

    @Override
    public boolean onFetchFirstPage(PeopleListFilter filter) {
        Blog blog = WordPress.getCurrentBlog();
        if (filter == PeopleListFilter.TEAM && !mHasRefreshedUsers) {
            return fetchUsersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), 0);
        } else if (filter == PeopleListFilter.FOLLOWERS && !mHasRefreshedFollowers) {
            return fetchFollowersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), 1);
        } else if (filter == PeopleListFilter.EMAIL_FOLLOWERS && !mHasRefreshedEmailFollowers) {
            return fetchEmailFollowersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), 1);
        } else if (filter == PeopleListFilter.VIEWERS && !mHasRefreshedViewers) {
            return fetchViewersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), 0);
        }
        return false;
    }

    @Override
    public boolean onFetchMorePeople(PeopleListFilter filter) {
        if (filter == PeopleListFilter.TEAM && !mUsersEndOfListReached) {
            Blog blog = WordPress.getCurrentBlog();
            int count = PeopleTable.getUsersCountForLocalBlogId(blog.getLocalTableBlogId());
            return fetchUsersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), count);
        } else if (filter == PeopleListFilter.FOLLOWERS && !mFollowersEndOfListReached) {
            Blog blog = WordPress.getCurrentBlog();
            int pageToFetch = mFollowersLastFetchedPage + 1;
            return fetchFollowersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), pageToFetch);
        } else if (filter == PeopleListFilter.EMAIL_FOLLOWERS && !mEmailFollowersEndOfListReached) {
            Blog blog = WordPress.getCurrentBlog();
            int pageToFetch = mEmailFollowersLastFetchedPage + 1;
            return fetchEmailFollowersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), pageToFetch);
        } else if (filter == PeopleListFilter.VIEWERS && !mViewersEndOfListReached) {
            Blog blog = WordPress.getCurrentBlog();
            int count = PeopleTable.getViewersCountForLocalBlogId(blog.getLocalTableBlogId());
            return fetchViewersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), count);
        }
        return false;
    }

    private PeopleListFragment getListFragment() {
        return (PeopleListFragment) getFragmentManager().findFragmentByTag(KEY_PEOPLE_LIST_FRAGMENT);
    }

    private PersonDetailFragment getDetailFragment() {
        return (PersonDetailFragment) getFragmentManager().findFragmentByTag(KEY_PERSON_DETAIL_FRAGMENT);
    }

    public interface InvitationSender {
        void send();
    }
}
