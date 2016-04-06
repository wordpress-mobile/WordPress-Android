package org.wordpress.android.ui.people;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

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

public class PeopleManagementActivity extends AppCompatActivity {

    private PeopleAdapter mPeopleAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        if (blog != null) {
            ListView listView = (ListView)findViewById(android.R.id.list);
            List<Person> peopleList = PeopleTable.getPeople(localBlogId);
            mPeopleAdapter = new PeopleAdapter(this, peopleList);
            listView.setAdapter(mPeopleAdapter);

            final Activity context = this;
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Person person = (Person) parent.getItemAtPosition(position);
                    ActivityLauncher.viewPersonDetails(context, person);
                }
            });

            refreshUsersList(blog.getDotComBlogId(), localBlogId);
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
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
                mPeopleAdapter.setPeopleList(peopleList);
                mPeopleAdapter.notifyDataSetChanged();
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
}
