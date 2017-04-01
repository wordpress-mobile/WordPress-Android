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
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;

import javax.inject.Inject;

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
    private boolean mRefreshedUsersEh;
    private boolean mRefreshedFollowersEh;
    private boolean mRefreshedEmailFollowersEh;
    private boolean mRefreshedViewersEh;

    // If we are currently making a request for a certain filter
    private boolean mUsersFetchRequestInProgress;
    private boolean mFollowersFetchRequestInProgress;
    private boolean mEmailFollowersFetchRequestInProgress;
    private boolean mViewersFetchRequestInProgress;

    // Keep track of the last page we received from remote
    private int mFollowersLastFetchedPage;
    private int mEmailFollowersLastFetchedPage;

    @Inject AccountStore mAccountStore;

    private SiteModel mSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.people_management_activity);
        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(0);
        }


        FragmentManager fragmentManager = getFragmentManager();

        if (savedInstanceState == null) {
            // only delete cached people if there is a connection
            if (NetworkUtils.networkAvailableEh(this)) {
                PeopleTable.deletePeopleExceptForFirstPage(mSite.getId());
            }

            if (actionBar != null) {
                actionBar.setTitle(R.string.people);
            }

            PeopleListFragment peopleListFragment = PeopleListFragment.newInstance(mSite);
            peopleListFragment.setOnPersonSelectedListener(this);
            peopleListFragment.setOnFetchPeopleListener(this);

            mUsersEndOfListReached = false;
            mFollowersEndOfListReached = false;
            mEmailFollowersEndOfListReached = false;
            mViewersEndOfListReached = false;

            mRefreshedUsersEh = false;
            mRefreshedFollowersEh = false;
            mRefreshedEmailFollowersEh = false;
            mRefreshedViewersEh = false;

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

            mRefreshedUsersEh = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_USERS);
            mRefreshedFollowersEh = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_FOLLOWERS);
            mRefreshedEmailFollowersEh = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_EMAIL_FOLLOWERS);
            mRefreshedViewersEh = savedInstanceState.getBoolean(KEY_HAS_REFRESHED_VIEWERS);

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
        outState.putSerializable(WordPress.SITE, mSite);

        outState.putBoolean(KEY_USERS_END_OF_LIST_REACHED, mUsersEndOfListReached);
        outState.putBoolean(KEY_FOLLOWERS_END_OF_LIST_REACHED, mFollowersEndOfListReached);
        outState.putBoolean(KEY_EMAIL_FOLLOWERS_END_OF_LIST_REACHED, mEmailFollowersEndOfListReached);
        outState.putBoolean(KEY_VIEWERS_END_OF_LIST_REACHED, mViewersEndOfListReached);

        outState.putBoolean(KEY_HAS_REFRESHED_USERS, mRefreshedUsersEh);
        outState.putBoolean(KEY_HAS_REFRESHED_FOLLOWERS, mRefreshedFollowersEh);
        outState.putBoolean(KEY_HAS_REFRESHED_EMAIL_FOLLOWERS, mRefreshedEmailFollowersEh);
        outState.putBoolean(KEY_HAS_REFRESHED_VIEWERS, mRefreshedViewersEh);

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
                peopleInviteFragment = PeopleInviteFragment.newInstance(mSite);
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

    private boolean fetchUsersList(final SiteModel site, final int offset) {
        if (mUsersEndOfListReached || mUsersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mUsersFetchRequestInProgress = true;

        PeopleUtils.fetchUsers(site, offset, new PeopleUtils.FetchUsersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, boolean endOfListEh) {
                boolean freshListEh = (offset == 0);
                mRefreshedUsersEh = true;
                mUsersEndOfListReached = endOfListEh;
                PeopleTable.saveUsers(peopleList, site.getId(), freshListEh);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.TEAM, freshListEh, true);
                }

                refreshOnScreenFragmentDetails();
                mUsersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean firstPageEh = offset == 0;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.TEAM, firstPageEh, false);
                }
                mUsersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_users_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    private boolean fetchFollowersList(final SiteModel site, final int page) {
        if (mFollowersEndOfListReached || mFollowersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mFollowersFetchRequestInProgress = true;

        PeopleUtils.fetchFollowers(site, page, new PeopleUtils.FetchFollowersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, int pageFetched, boolean endOfListEh) {
                boolean freshListEh = (page == 1);
                mRefreshedFollowersEh = true;
                mFollowersLastFetchedPage = pageFetched;
                mFollowersEndOfListReached = endOfListEh;
                PeopleTable.saveFollowers(peopleList, site.getId(), freshListEh);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.FOLLOWERS, freshListEh, true);
                }

                refreshOnScreenFragmentDetails();
                mFollowersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean firstPageEh = page == 1;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.FOLLOWERS, firstPageEh, false);
                }
                mFollowersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_followers_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    private boolean fetchEmailFollowersList(final SiteModel site, final int page) {
        if (mEmailFollowersEndOfListReached || mEmailFollowersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mEmailFollowersFetchRequestInProgress = true;

        PeopleUtils.fetchEmailFollowers(site, page, new PeopleUtils.FetchFollowersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, int pageFetched, boolean endOfListEh) {
                boolean freshListEh = (page == 1);
                mRefreshedEmailFollowersEh = true;
                mEmailFollowersLastFetchedPage = pageFetched;
                mEmailFollowersEndOfListReached = endOfListEh;
                PeopleTable.saveEmailFollowers(peopleList, site.getId(), freshListEh);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.EMAIL_FOLLOWERS, freshListEh, true);
                }

                refreshOnScreenFragmentDetails();
                mEmailFollowersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean firstPageEh = page == 1;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.EMAIL_FOLLOWERS, firstPageEh, false);
                }
                mEmailFollowersFetchRequestInProgress = false;
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_email_followers_list,
                        ToastUtils.Duration.SHORT);
            }
        });

        return true;
    }

    private boolean fetchViewersList(final SiteModel site, final int offset) {
        if (mViewersEndOfListReached || mViewersFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return false;
        }

        mViewersFetchRequestInProgress = true;

        PeopleUtils.fetchViewers(site, offset, new PeopleUtils.FetchViewersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, boolean endOfListEh) {
                boolean freshListEh = (offset == 0);
                mRefreshedViewersEh = true;
                mViewersEndOfListReached = endOfListEh;
                PeopleTable.saveViewers(peopleList, site.getId(), freshListEh);

                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.VIEWERS, freshListEh, true);
                }

                refreshOnScreenFragmentDetails();
                mViewersFetchRequestInProgress = false;
            }

            @Override
            public void onError() {
                PeopleListFragment peopleListFragment = getListFragment();
                if (peopleListFragment != null) {
                    boolean firstPageEh = offset == 0;
                    peopleListFragment.fetchingRequestFinished(PeopleListFilter.VIEWERS, firstPageEh, false);
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
            personDetailFragment = PersonDetailFragment.newInstance(mAccountStore.getAccount().getUserId(), personID,
                    localTableBlogID, person.getPersonType());
        } else {
            personDetailFragment.setPersonDetails(personID, localTableBlogID);
        }
        if (!personDetailFragment.isAdded()) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.OPENED_PERSON, mSite);
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

        final PersonDetailFragment personDetailFragment = getDetailFragment();
        if (personDetailFragment != null) {
            // optimistically update the role
            personDetailFragment.changeRole(event.newRole);
        }

        PeopleUtils.updateRole(mSite, person.getPersonID(), event.newRole, event.localTableBlogId,
                new PeopleUtils.UpdateUserCallback() {
            @Override
            public void onSuccess(Person person) {
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PERSON_UPDATED, mSite);
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

        final Person.PersonType personType = person.getPersonType();
        final String displayName = person.getDisplayName();

        PeopleUtils.RemovePersonCallback callback = new PeopleUtils.RemovePersonCallback() {
            @Override
            public void onSuccess(long personID, int localTableBlogId) {
                if (personType == Person.PersonType.USER) {
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.PERSON_REMOVED, mSite);
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
            PeopleUtils.removeFollower(mSite, person.getPersonID(), personType, callback);
        } else if(personType == Person.PersonType.VIEWER) {
            PeopleUtils.removeViewer(mSite, person.getPersonID(), callback);
        } else {
            PeopleUtils.removeUser(mSite, person.getPersonID(),callback);
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
        if (filter == PeopleListFilter.TEAM && !mRefreshedUsersEh) {
            return fetchUsersList(mSite, 0);
        } else if (filter == PeopleListFilter.FOLLOWERS && !mRefreshedFollowersEh) {
            return fetchFollowersList(mSite, 1);
        } else if (filter == PeopleListFilter.EMAIL_FOLLOWERS && !mRefreshedEmailFollowersEh) {
            return fetchEmailFollowersList(mSite, 1);
        } else if (filter == PeopleListFilter.VIEWERS && !mRefreshedViewersEh) {
            return fetchViewersList(mSite, 0);
        }
        return false;
    }

    @Override
    public boolean onFetchMorePeople(PeopleListFilter filter) {
        if (filter == PeopleListFilter.TEAM && !mUsersEndOfListReached) {
            int count = PeopleTable.getUsersCountForLocalBlogId(mSite.getId());
            return fetchUsersList(mSite, count);
        } else if (filter == PeopleListFilter.FOLLOWERS && !mFollowersEndOfListReached) {
            int pageToFetch = mFollowersLastFetchedPage + 1;
            return fetchFollowersList(mSite, pageToFetch);
        } else if (filter == PeopleListFilter.EMAIL_FOLLOWERS && !mEmailFollowersEndOfListReached) {
            int pageToFetch = mEmailFollowersLastFetchedPage + 1;
            return fetchEmailFollowersList(mSite, pageToFetch);
        } else if (filter == PeopleListFilter.VIEWERS && !mViewersEndOfListReached) {
            int count = PeopleTable.getViewersCountForLocalBlogId(mSite.getId());
            return fetchViewersList(mSite, count);
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
