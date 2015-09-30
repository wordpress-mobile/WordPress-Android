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
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Map;

public class StatsWidgetConfigureActivity2 extends AppCompatActivity
        implements SitePickerAdapter.OnSiteClickListener {

    private SitePickerAdapter mAdapter;
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
            ToastUtils.showToast(getBaseContext(), R.string.stats_widget_error_login, ToastUtils.Duration.LONG);
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

        setContentView(R.layout.site_picker_activity);

        final boolean isInSearchMode = false;
        String lastSearch = "";
        setNewAdapter(lastSearch, isInSearchMode);

        setupActionBar();
        setupRecycleView();
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

    private SitePickerAdapter getAdapter() {
        if (mAdapter == null) {
            setNewAdapter("", false);
        }
        return mAdapter;
    }

    private void setNewAdapter(String lastSearch, boolean isInSearchMode) {
        mAdapter = new SitePickerAdapter(this, 0, lastSearch, isInSearchMode);
        mAdapter.setOnSiteClickListener(this);
    }

    @Override
    public void onSiteClick(SitePickerAdapter.SiteRecord site) {
        addWidgetToScreenAndFinish(site.localId);
    }

    private void addWidgetToScreenAndFinish(int localID) {
        final Context context = StatsWidgetConfigureActivity2.this;
        StatsWidgetProvider.setupNewWidget(context, mAppWidgetId, localID);
        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
