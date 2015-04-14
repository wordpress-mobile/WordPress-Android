package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

public class SitePickerActivity extends ActionBarActivity {

    public static final String KEY_LOCAL_ID = "local_id";
    private static final String KEY_BLOG_ID = "blog_id";
    public static final String ARG_VISIBLE_ONLY = "visible_blogs_only";

    private RecyclerView mRecycler;
    private int mBlavatarSz;
    private boolean mVisibleBlogsOnly;
    private SiteList mSiteList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.site_picker_activity);
        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz);

        if (savedInstanceState != null) {
            mVisibleBlogsOnly = savedInstanceState.getBoolean(ARG_VISIBLE_ONLY);
        } else {
            mVisibleBlogsOnly = getIntent().getBooleanExtra(ARG_VISIBLE_ONLY, false);
        }

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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_VISIBLE_ONLY, mVisibleBlogsOnly);
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
            List<Map<String, Object>> blogs;
            if (mVisibleBlogsOnly) {
                blogs = WordPress.wpDB.getVisibleDotComBlogs();
            } else {
                blogs = WordPress.wpDB.getBlogsBy("dotcomFlag=1", new String[]{"isHidden"});
            }

            // include self-hosted, then sort all by name
            blogs.addAll(WordPress.wpDB.getBlogsBy("dotcomFlag!=1", null));
            Collections.sort(blogs, BlogUtils.BlogNameComparator);

            return new SiteList(blogs);
        }

        @Override
        protected void onPostExecute(SiteList sites) {
            if (mSiteList == null || !mSiteList.isSameList(sites)) {
                mSiteList = sites;
                mRecycler.setAdapter(new SiteAdapter(SitePickerActivity.this, mSiteList));
            }
            mIsTaskRunning = false;
        }
    }

    class SiteAdapter extends RecyclerView.Adapter<SiteViewHolder> {
        private final SiteList mSites;
        private final LayoutInflater mInflater;

        public SiteAdapter(Context context, @NonNull SiteList sites) {
            super();
            setHasStableIds(true);
            mInflater = LayoutInflater.from(context);
            mSites = sites;
        }

        @Override
        public int getItemCount() {
            return mSites.size();
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).localId;
        }

        SiteRecord getItem(int position) {
            return mSites.get(position);
        }

        @Override
        public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.site_picker_listitem, parent, false);
            return new SiteViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SiteViewHolder holder, final int position) {
            SiteRecord site = getItem(position);
            holder.txtTitle.setText(site.blogName);
            holder.txtDomain.setText(site.hostName);
            holder.imgBlavatar.setImageUrl(site.blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemSelected(getItem(position));
                }
            });
        }
    }

    static class SiteViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtDomain;
        private final WPNetworkImageView imgBlavatar;

        public SiteViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDomain = (TextView) view.findViewById(R.id.text_domain);
            imgBlavatar = (WPNetworkImageView) view.findViewById(R.id.image_blavatar);
        }
    }

    /**
     * SiteRecord is a simplified version of the full account record optimized for use
     * with the above site adapter
     */
    class SiteRecord {
        final int localId;
        final String blogId;
        final String blogName;
        final String hostName;
        final String url;
        final String blavatarUrl;

        SiteRecord(Map<String, Object> account) {
            localId = MapUtils.getMapInt(account, "id");
            blogId = MapUtils.getMapStr(account, "blogId");
            blogName = BlogUtils.getBlogNameFromAccountMap(account);
            hostName = BlogUtils.getHostNameFromAccountMap(account);
            url = MapUtils.getMapStr(account, "url");
            blavatarUrl = GravatarUtils.blavatarFromUrl(url, mBlavatarSz);
        }
    }

    class SiteList extends ArrayList<SiteRecord> {
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
}
