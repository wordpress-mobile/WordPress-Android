package org.wordpress.android.ui.people;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.android.volley.VolleyError;

import org.json.JSONException;
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

    private PeopleListFragment mPeopleListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int localBlogId = BlogUtils.getBlogLocalId(WordPress.getCurrentBlog());
        Blog blog = WordPress.getBlog(localBlogId);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.people_management_activity);

        setTitle(R.string.people);

        FragmentManager fragmentManager = getFragmentManager();
        if (mPeopleListFragment == null) {
            mPeopleListFragment = PeopleListFragment.newInstance();

            fragmentManager.beginTransaction()
                    .add(android.R.id.content, mPeopleListFragment)
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

    private void refreshUsersList(String dotComBlogId, final int localBlogId) {
        PeopleUtils.fetchUsers(dotComBlogId, localBlogId, new PeopleUtils.Callback() {
            @Override
            public void onSuccess(List<Person> peopleList) {
                PeopleTable.savePeople(peopleList);
                mPeopleListFragment.setPeopleList(peopleList);
            }

            @Override
            public void onError(VolleyError error) {
                //TODO: show some kind of error to the user
            }

            @Override
            public void onJSONException(JSONException exception) {
                //TODO: show some kind of error to the user
            }
        });
    }

    @Override
    public void onPersonSelected(Person person) {
        PersonDetailFragment fragment = PersonDetailFragment.newInstance();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();
        fragment.setPerson(person);
    }
}
