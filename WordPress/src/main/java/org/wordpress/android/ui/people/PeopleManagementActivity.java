package org.wordpress.android.ui.people;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;

public class PeopleManagementActivity extends AppCompatActivity
        implements PeopleListFragment.OnPersonSelectedListener, PersonDetailFragment.OnChangeListener {
    private static final String KEY_PEOPLE_LIST_FRAGMENT = "people-list-fragment";
    private static final String KEY_PERSON_DETAIL_FRAGMENT = "person-detail-fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.people_management_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.people);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        int localBlogId = BlogUtils.getBlogLocalId(WordPress.getCurrentBlog());
        Blog blog = WordPress.getBlog(localBlogId);

        if (savedInstanceState == null) {
            PeopleListFragment peopleListFragment = PeopleListFragment.newInstance(localBlogId);

            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, peopleListFragment, KEY_PEOPLE_LIST_FRAGMENT)
                    .commit();
        }

        if (blog != null) {
            refreshUsersList(blog.getDotComBlogId(), localBlogId);
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0 ){
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshUsersList(String dotComBlogId, final int localTableBlogId) {
        PeopleUtils.fetchUsers(dotComBlogId, localTableBlogId, new PeopleUtils.FetchUsersCallback() {
            @Override
            public void onSuccess(List<Person> peopleList) {
                PeopleTable.savePeople(peopleList);
                refreshOnScreenFragmentDetails();
            }

            @Override
            public void onError() {
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_fetch_people_list,
                        ToastUtils.Duration.LONG);
            }
        });
    }

    @Override
    public void onPersonSelected(Person person) {
        FragmentManager fragmentManager = getFragmentManager();
        PersonDetailFragment personDetailFragment = (PersonDetailFragment) fragmentManager
                .findFragmentByTag(KEY_PERSON_DETAIL_FRAGMENT);

        long personID = person.getPersonID();
        int localTableBlogID = person.getLocalTableBlogId();
        if (personDetailFragment == null) {
            personDetailFragment = PersonDetailFragment.newInstance(personID, localTableBlogID);
        } else {
            personDetailFragment.setPersonDetails(personID, localTableBlogID);
        }
        if (!personDetailFragment.isAdded()) {
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, personDetailFragment, KEY_PERSON_DETAIL_FRAGMENT);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onRoleChanged(long personID, int localTableBlogId, String newRole) {
        Person person = PeopleTable.getPerson(personID, localTableBlogId);
        if (person == null || newRole == null || newRole.equalsIgnoreCase(person.getRole())) {
            return;
        }
        PeopleUtils.updateRole(person.getBlogId(), person.getPersonID() + "", newRole, localTableBlogId, new PeopleUtils.UpdateUserCallback() {
            @Override
            public void onSuccess(Person person) {
                PeopleTable.save(person);
                refreshOnScreenFragmentDetails();
            }

            @Override
            public void onError() {
                ToastUtils.showToast(PeopleManagementActivity.this,
                        R.string.error_update_role,
                        ToastUtils.Duration.LONG);
            }
        });
    }

    // This helper method is used after a successful network request
    private void refreshOnScreenFragmentDetails() {
        FragmentManager fragmentManager = getFragmentManager();
        PeopleListFragment peopleListFragment = (PeopleListFragment) fragmentManager
                .findFragmentByTag(KEY_PEOPLE_LIST_FRAGMENT);
        PersonDetailFragment personDetailFragment = (PersonDetailFragment) fragmentManager
                .findFragmentByTag(KEY_PERSON_DETAIL_FRAGMENT);

        if (peopleListFragment != null) {
            peopleListFragment.refreshPeopleList();
        }
        if (personDetailFragment != null) {
            personDetailFragment.refreshPersonDetails();
        }
    }
}
