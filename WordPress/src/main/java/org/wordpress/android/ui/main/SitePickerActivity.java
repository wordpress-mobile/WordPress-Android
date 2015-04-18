package org.wordpress.android.ui.main;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.widgets.DividerItemDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class SitePickerActivity extends ActionBarActivity {

    public static final String KEY_LOCAL_ID = "local_id";
    private static final String KEY_BLOG_ID = "blog_id";

    private RecyclerView mRecycler;
    private static int mBlavatarSz;

    private SitePickerAdapter mSiteAdapter;
    private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.site_picker_activity);
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mRecycler = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        Button btnAddSite = (Button) findViewById(R.id.btn_add_site);
        btnAddSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityLauncher.addSelfHostedSiteForResult(SitePickerActivity.this);
            }
        });

        loadSites();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_left);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onItemSelected(@NonNull SiteRecord site) {
        Intent data = new Intent();
        data.putExtra(KEY_LOCAL_ID, site.localId);
        data.putExtra(KEY_BLOG_ID, site.blogId);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SignInActivity.CREATE_ACCOUNT_REQUEST:
                if (resultCode != RESULT_CANCELED) {
                    loadSites();
                }
                break;
        }
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        if (!isFinishing()) {
            loadSites();
        }
    }

    private void loadSites() {
        if (!mIsTaskRunning) {
            new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private boolean hasAdapter() {
        return mSiteAdapter != null;
    }

    private SitePickerAdapter newAdapter(SiteList sites) {
        mSiteAdapter = new SitePickerAdapter(this, sites);
        mSiteAdapter.setOnSelectedItemsChangeListener(new SitePickerAdapter.OnSelectedItemsChangeListener() {
            @Override
            public void onSelectedItemsChanged() {
                if (mActionMode != null) {
                    if (mSiteAdapter.getSelectionCount() == 0) {
                        mActionMode.finish();
                    } else {
                        updateActionModeTitle();
                        mActionMode.invalidate();
                    }
                }
            }
        });
        return mSiteAdapter;
    }

    private void updateActionModeTitle() {
        if (mActionMode == null || !hasAdapter()) return;

        int numSelected = mSiteAdapter.getSelectionCount();
        if (numSelected > 0) {
            mActionMode.setTitle(Integer.toString(numSelected));
        } else {
            mActionMode.setTitle("");
        }
    }

    /*
     * AsyncTask which loads site data from local db and populates the site list
     */
    private boolean mIsTaskRunning;
    private class LoadSitesTask extends AsyncTask<Void, Void, SiteList> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsTaskRunning = false;
        }

        @Override
        protected SiteList doInBackground(Void... params) {
            // get wp.com blogs
            List<Map<String, Object>> blogs = WordPress.wpDB.getBlogsBy("dotcomFlag=1", new String[]{"isHidden"});

            // include self-hosted
            blogs.addAll(WordPress.wpDB.getBlogsBy("dotcomFlag!=1", null));

            SiteList sites = new SiteList(blogs);
            Collections.sort(sites, SiteComparator);

            return sites;
        }

        @Override
        protected void onPostExecute(SiteList sites) {
            if (!hasAdapter() || !mSiteAdapter.isSameList(sites)) {
                mRecycler.setAdapter(newAdapter(sites));
            }
            mIsTaskRunning = false;
        }
    }


    /**
     * SiteRecord is a simplified version of the full account record
     */
    static class SiteRecord {
        final int localId;
        final String blogId;
        final String blogName;
        final String hostName;
        final String url;
        final String blavatarUrl;
        final boolean isHidden;

        SiteRecord(Map<String, Object> account) {
            localId = MapUtils.getMapInt(account, "id");
            blogId = MapUtils.getMapStr(account, "blogId");
            blogName = BlogUtils.getBlogNameFromAccountMap(account);
            hostName = BlogUtils.getHostNameFromAccountMap(account);
            url = MapUtils.getMapStr(account, "url");
            blavatarUrl = GravatarUtils.blavatarFromUrl(url, mBlavatarSz);
            isHidden = MapUtils.getMapBool(account, "isHidden");
        }

        String getBlogNameOrHostName() {
            if (TextUtils.isEmpty(blogName)) {
                return hostName;
            }
            return blogName;
        }
    }

    static class SiteList extends ArrayList<SiteRecord> {
        SiteList() { }
        SiteList(List<Map<String, Object>> accounts) {
            if (accounts != null) {
                for (Map<String, Object> account : accounts) {
                    add(new SiteRecord(account));
                }
            }
        }

        boolean isSameList(SiteList sites) {
            if (sites == null || sites.size() != this.size()) {
                return false;
            }
            for (SiteRecord site: sites) {
                if (!this.containsSite(site)) {
                    return false;
                }
            }
            return true;
        }

        boolean containsSite(SiteRecord site) {
            if (site != null && site.blogId != null) {
                for (SiteRecord thisSite : this) {
                    if (site.blogId.equals(thisSite.blogId)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /*
     * sorts sites based on their name/host and visibility - hidden blogs are sorted
     * below visible ones
     */
    private static final Comparator<SiteRecord> SiteComparator = new Comparator<SiteRecord>() {
        public int compare(SiteRecord site1, SiteRecord site2) {
            if (site1.isHidden != site2.isHidden) {
                return (site1.isHidden ? 1 : -1);
            } else {
                return site1.getBlogNameOrHostName().compareToIgnoreCase(site2.getBlogNameOrHostName());
            }
        }
    };
}
