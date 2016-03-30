package org.wordpress.android.ui.people;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.BlogUtils;
import org.wordpress.android.ui.people.utils.PeopleUtils;

public class PeopleManagementActivity extends AppCompatActivity {

    private int mBlogLocalId = BlogUtils.BLOG_ID_INVALID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBlogLocalId = BlogUtils.getBlogLocalId(WordPress.getCurrentBlog());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.people_management_activity);

        setTitle(R.string.people);

        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setAdapter(new PeopleAdapter(this));

        final Activity context = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Person person = (Person) parent.getItemAtPosition(position);
                ActivityLauncher.viewPersonDetails(context, person);
            }
        });

        refreshUsersList();
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

    private void refreshUsersList() {
        Blog blog = WordPress.getBlog(mBlogLocalId);
        if (blog != null) {
            PeopleUtils.fetchUsers(blog.getDotComBlogId(), null);
        }
    }
}
