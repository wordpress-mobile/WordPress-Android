package org.wordpress.android.ui.stats;

/**
 * The configuration screen for the StatsWidgetProvider widget.
 */

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Map;

public class StatsWidgetConfigureActivity extends AppCompatActivity
        implements StatsWidgetConfigureAdapter.OnSiteClickListener {

    private StatsWidgetConfigureAdapter mAdapter;
    private RecyclerView mRecycleView;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Set the result to CANCELED.  This will cause the widget host to cancel out of the widget
        // placement if they press the back button.
        setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));

        // Intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // If not signed into WordPress inform the user
        if (!AccountHelper.isSignedIn()) {
            ToastUtils.showToast(getBaseContext(), R.string.stats_widget_error_no_account, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        // If no visible blogs
        List<Map<String, Object>> accounts = WordPress.wpDB.getBlogsBy("isHidden = 0", null);
        if (accounts.size() == 0) {
            ToastUtils.showToast(getBaseContext(), R.string.stats_widget_error_no_visible_blog, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        // If one blog only, skip config
        if (accounts.size() == 1) {
            Map<String, Object> account = accounts.get(0);
            Integer localID = (Integer) account.get("id");
            addWidgetToScreenAndFinish(localID);
            return;
        }

        setContentView(R.layout.stats_widget_config_activity);
        setNewAdapter();
        setupActionBar();
        setupRecycleView();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void setupRecycleView() {
        mRecycleView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));
        mRecycleView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mRecycleView.setItemAnimator(null);
        mRecycleView.setAdapter(getAdapter());
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private StatsWidgetConfigureAdapter getAdapter() {
        if (mAdapter == null) {
            setNewAdapter();
        }
        return mAdapter;
    }

    private void setNewAdapter() {
        Blog blog = WordPress.getCurrentBlog();
        int localBlogId = (blog != null ? blog.getLocalTableBlogId() : 0);
        mAdapter = new StatsWidgetConfigureAdapter(this, localBlogId);
        mAdapter.setOnSiteClickListener(this);
    }

    @Override
    public void onSiteClick(StatsWidgetConfigureAdapter.SiteRecord site) {
        addWidgetToScreenAndFinish(site.localId);
    }

    private void addWidgetToScreenAndFinish(int localID) {
        final Blog currentBlog = WordPress.getBlog(localID);

        if (currentBlog == null) {
            AppLog.e(AppLog.T.STATS, "The blog with local_blog_id " + localID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (currentBlog.getDotComBlogId() == null) {
            // The blog could be a self-hosted blog with NO Jetpack installed on it
            // Or a Jetpack blog whose options are not yet synched in the app
            // In both of these cases show a generic message that encourages the user to refresh
            // the blog within the app. There are so many different paths here that's better to handle them in the app.
            Toast.makeText(this, R.string.stats_widget_error_jetpack_no_blogid, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final Context context = StatsWidgetConfigureActivity.this;
        StatsWidgetProvider.setupNewWidget(context, mAppWidgetId, localID);
        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
