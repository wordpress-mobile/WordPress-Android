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
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.widgets.DividerItemDecoration;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SitePickerActivity extends ActionBarActivity {

    public static final String KEY_LOCAL_ID = "local_id";
    private static final String KEY_BLOG_ID  = "blog_id";

    private RecyclerView mRecycler;
    private int mBlavatarSz;

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

        new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    void onItemSelected(@NonNull SiteRecord site) {
        Intent data = new Intent();
        data.putExtra(KEY_LOCAL_ID, site.localId);
        data.putExtra(KEY_BLOG_ID, site.blogId);
        setResult(RESULT_OK, data);
        finish();
    }

    private class LoadSitesTask extends AsyncTask<Void, Void, Boolean> {
        private SiteList mSites;

        @Override
        protected Boolean doInBackground(Void... params) {
            List<Map<String, Object>> accounts = WordPress.wpDB.getAccountsBy("dotcomFlag=1", new String[]{"isHidden"});
            mSites = new SiteList(accounts);
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                SiteAdapter adapter = new SiteAdapter(SitePickerActivity.this, mSites);
                mRecycler.setAdapter(adapter);
            }
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
            for (Map<String, Object> account: accounts) {
                add(new SiteRecord(account));
            }
        }
    }
}
