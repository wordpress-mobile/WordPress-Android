package org.wordpress.android.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;
import java.util.Map;

public class SitePickerActivity extends ActionBarActivity {

    private RecyclerView mRecycler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.site_picker_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mRecycler = (RecyclerView) findViewById(R.id.recycler_view);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        new LoadAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    void showProgress(boolean show) {
        if (isFinishing()) {
            return;
        }
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(show ? View.VISIBLE : View. GONE);
    }

    private class LoadAccountsTask extends AsyncTask<Void, Void, Boolean> {
        private List<Map<String, Object>> accounts;

        @Override
        protected void onPreExecute() {
            showProgress(true);
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            accounts = WordPress.wpDB.getAccountsBy("dotcomFlag=1", new String[]{"isHidden"});
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                SiteAdapter adapter = new SiteAdapter(SitePickerActivity.this, accounts);
                mRecycler.setAdapter(adapter);
            }
            showProgress(false);
        }
    }

    class SiteAdapter extends RecyclerView.Adapter<SiteViewHolder> {

        private List<Map<String, Object>> mAccounts;
        private final int mBlavatarSz;
        private final LayoutInflater mInflater;

        public SiteAdapter(Context context, List<Map<String, Object>> accounts) {
            super();
            mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium);
            mInflater = LayoutInflater.from(context);
            mAccounts = accounts;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.site_picker_listitem, parent, false);
            return new SiteViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SiteViewHolder holder, final int position) {
            Map<String, Object> item = mAccounts.get(position);
            holder.txtTitle.setText(BlogUtils.getBlogNameFromAccountMap(item));
            holder.txtDomain.setText(BlogUtils.getHostNameFromAccountMap(item));
            String url = MapUtils.getMapStr(item, "url");
            holder.imgBlavatar.setImageUrl(
                    GravatarUtils.blavatarFromUrl(url, mBlavatarSz),
                    WPNetworkImageView.ImageType.BLAVATAR);
        }

        @Override
        public int getItemCount() {
            return mAccounts.size();
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

}
