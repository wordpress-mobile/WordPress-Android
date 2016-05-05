package org.wordpress.android.ui.people;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils;

import java.util.List;

public class PeopleManagementActivity extends AppCompatActivity implements PeopleListFragment.OnPersonSelectedListener {
    private static final String KEY_PEOPLE_LIST_FRAGMENT = "people-list-fragment";
    private static final String KEY_PERSON_DETAIL_FRAGMENT = "person-detail-fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.people_management_activity);

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

            @Override
            public void onError() {
                //TODO: show some kind of error to the user
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
}
