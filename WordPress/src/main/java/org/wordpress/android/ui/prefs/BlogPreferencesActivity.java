package org.wordpress.android.ui.prefs;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.StringUtils;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends AppCompatActivity {
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";
    public static final int RESULT_BLOG_REMOVED = RESULT_FIRST_USER;

    // The blog this activity is managing settings for.
    private Blog blog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Integer id = getIntent().getIntExtra(ARG_LOCAL_BLOG_ID, -1);
        blog = WordPress.getBlog(id);

        if (blog == null) {
            Toast.makeText(this, getString(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Fragment siteSettingsFragment = new SiteSettingsFragment();
        siteSettingsFragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, siteSettingsFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setTitle(StringUtils.unescapeHTML(blog.getNameOrHostUrl()));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        WordPress.wpDB.saveBlog(blog);

        if (WordPress.getCurrentBlog().getLocalTableBlogId() == blog.getLocalTableBlogId()) {
            WordPress.currentBlog = blog;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
