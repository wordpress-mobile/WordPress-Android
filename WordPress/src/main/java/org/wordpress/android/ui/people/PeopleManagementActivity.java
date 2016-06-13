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
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;

import de.greenrobot.event.EventBus;


public class PeopleManagementActivity extends AppCompatActivity
        implements PeopleListFragment.OnPersonSelectedListener, PeopleListFragment.OnFetchPeopleListener {
    private static final String KEY_PEOPLE_LIST_FRAGMENT = "people-list-fragment";
    private static final String KEY_PERSON_DETAIL_FRAGMENT = "person-detail-fragment";
    private static final String KEY_END_OF_LIST_REACHED = "end-of-list-reached";
    private static final String KEY_FETCH_REQUEST_IN_PROGRESS = "fetch-request-in-progress";
    private static final String KEY_TITLE = "page-title";
    private static final String KEY_PEOPLE_INVITE_FRAGMENT = "people-invite-fragment";

    private boolean mPeopleEndOfListReached;
    private boolean mFetchRequestInProgress;

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
                PeopleTable.deletePeopleForLocalBlogIdExceptForFirstPage(blog.getLocalTableBlogId());
            }

            if (actionBar != null) {
                actionBar.setTitle(R.string.people);
            }

            PeopleListFragment peopleListFragment = PeopleListFragment.newInstance(blog.getLocalTableBlogId());
            peopleListFragment.setOnPersonSelectedListener(this);
            peopleListFragment.setOnFetchPeopleListener(this);

            mPeopleEndOfListReached = false;
            mFetchRequestInProgress = false;

            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, peopleListFragment, KEY_PEOPLE_LIST_FRAGMENT)
                    .commit();
        } else {
            mPeopleEndOfListReached = savedInstanceState.getBoolean(KEY_END_OF_LIST_REACHED);
            mFetchRequestInProgress = savedInstanceState.getBoolean(KEY_FETCH_REQUEST_IN_PROGRESS);
            CharSequence title = savedInstanceState.getCharSequence(KEY_TITLE);

            if (actionBar != null && title != null) {
                actionBar.setTitle(title);
            }

            PeopleListFragment peopleListFragment = getListFragment();
            if (peopleListFragment != null) {
                peopleListFragment.setOnPersonSelectedListener(this);
                peopleListFragment.setOnFetchPeopleListener(this);
            }

            PersonDetailFragment personDetailFragment = getDetailFragment();
            if (personDetailFragment != null && personDetailFragment.isAdded()) {
                removeToolbarElevation();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_END_OF_LIST_REACHED, mPeopleEndOfListReached);
        outState.putBoolean(KEY_FETCH_REQUEST_IN_PROGRESS, mFetchRequestInProgress);
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
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
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
                peopleInviteFragment = PeopleInviteFragment.newInstance(blog.getDotComBlogId());
            }
            if (!peopleInviteFragment.isAdded()) {
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

    private void fetchUsersList(String dotComBlogId, final int localTableBlogId, final int offset) {
        if (mPeopleEndOfListReached || mFetchRequestInProgress || !NetworkUtils.checkConnection(this)) {
            return;
        }

        final PeopleListFragment peopleListFragment = getListFragment();
        if (peopleListFragment != null) {
            peopleListFragment.showLoadingProgress(true);
        }

        mFetchRequestInProgress = true;

        PeopleUtils.fetchUsers(dotComBlogId, localTableBlogId, offset, new PeopleUtils.FetchUsersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList, boolean isEndOfList) {
                boolean isFreshList = (offset == 0);
                mPeopleEndOfListReached = isEndOfList;
                PeopleTable.savePeople(peopleList, localTableBlogId, isFreshList);
                refreshOnScreenFragmentDetails();

                mFetchRequestInProgress = false;
                if (peopleListFragment != null) {
                    peopleListFragment.showLoadingProgress(false);
                }
            }

            @Override
            public void onError() {
                mFetchRequestInProgress = false;
                if (peopleListFragment != null) {
                    peopleListFragment.showLoadingProgress(false);
                }
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_people_list,
                        ToastUtils.Duration.LONG);
            }
        });
    }

    @Override
    public void onPersonSelected(Person person) {
        PersonDetailFragment personDetailFragment = getDetailFragment();

        long personID = person.getPersonID();
        int localTableBlogID = person.getLocalTableBlogId();
        if (personDetailFragment == null) {
            personDetailFragment = PersonDetailFragment.newInstance(personID, localTableBlogID);
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
            // remove the toolbar elevation for larger toolbar look
            removeToolbarElevation();

            fragmentTransaction.commit();
        }
    }

    public void onEventMainThread(RoleChangeDialogFragment.RoleChangeEvent event) {
        if(!NetworkUtils.checkConnection(this)) {
            return;
        }

        final Person person = PeopleTable.getPerson(event.personID, event.localTableBlogId);
        if (person == null || event.newRole == null || event.newRole.equalsIgnoreCase(person.getRole())) {
            return;
        }

        final PersonDetailFragment personDetailFragment = getDetailFragment();
        if (personDetailFragment != null) {
            // optimistically update the role
            personDetailFragment.changeRole(event.newRole);
        }

        PeopleUtils.updateRole(person.getBlogId(), person.getPersonID(), event.newRole, event.localTableBlogId,
                new PeopleUtils.UpdateUserCallback() {
            @Override
            public void onSuccess(Person person) {
                AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.PERSON_UPDATED);
                PeopleTable.save(person);
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
        builder.setMessage(getString(R.string.person_remove_confirmation_message, person.getDisplayName()));
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
        PeopleUtils.removePerson(person.getBlogId(),
                person.getPersonID(),
                person.getLocalTableBlogId(),
                new PeopleUtils.RemoveUserCallback() {
                    @Override
                    public void onSuccess(long personID, int localTableBlogId) {
                        AnalyticsUtils.trackWithCurrentBlogDetails(AnalyticsTracker.Stat.PERSON_REMOVED);
                        // remove the person from db, navigate back to list fragment and refresh it
                        Person person = PeopleTable.getPerson(personID, localTableBlogId);
                        String text;
                        if (person != null) {
                            PeopleTable.deletePerson(personID, localTableBlogId);
                            text = getString(R.string.person_removed, person.getUsername());
                        } else {
                            text = getString(R.string.person_removed_general);
                        }

                        ToastUtils.showToast(PeopleManagementActivity.this, text, ToastUtils.Duration.LONG);

                        navigateBackToPeopleListFragment();
                        refreshPeopleListFragment();
                    }

                    @Override
                    public void onError() {
                        ToastUtils.showToast(PeopleManagementActivity.this,
                                R.string.error_remove_user,
                                ToastUtils.Duration.LONG);
                    }
                });
    }

    // This helper method is used after a successful network request
    private void refreshOnScreenFragmentDetails() {
        refreshPeopleListFragment();
        refreshDetailFragment();
    }

    private void refreshPeopleListFragment() {
        PeopleListFragment peopleListFragment = getListFragment();
        if (peopleListFragment != null) {
            peopleListFragment.refreshPeopleList();
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

            // We need to reset the toolbar elevation if the user is navigating back from PersonDetailFragment
            PersonDetailFragment personDetailFragment = getDetailFragment();
            if (personDetailFragment != null && personDetailFragment.isAdded()) {
                resetToolbarElevation();

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
    public void onFetchFirstPage() {
        Blog blog = WordPress.getCurrentBlog();
        fetchUsersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), 0);
    }

    @Override
    public void onFetchMorePeople() {
        if (mPeopleEndOfListReached) {
            return;
        }
        Blog blog = WordPress.getCurrentBlog();
        int count = PeopleTable.getPeopleCountForLocalBlogId(blog.getLocalTableBlogId());
        fetchUsersList(blog.getDotComBlogId(), blog.getLocalTableBlogId(), count);
    }

    private PeopleListFragment getListFragment() {
        return (PeopleListFragment) getFragmentManager().findFragmentByTag(KEY_PEOPLE_LIST_FRAGMENT);
    }

    private PersonDetailFragment getDetailFragment() {
        return (PersonDetailFragment) getFragmentManager().findFragmentByTag(KEY_PERSON_DETAIL_FRAGMENT);
    }

    // Toolbar elevation is removed in detail fragment for larger toolbar look
    private void removeToolbarElevation() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
        }
    }

    private void resetToolbarElevation() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(getResources().getDimension(R.dimen.appbar_elevation));
        }
    }

    public interface InvitationSender {
        void send();
    }
}
