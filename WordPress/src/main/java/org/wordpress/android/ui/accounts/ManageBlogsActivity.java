package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.helpers.UpdateBlogListTask;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.helpers.ListScrollPositionManager;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;

import java.util.List;
import java.util.Map;

public class ManageBlogsActivity extends ActionBarActivity {
    private List<Map<String, Object>> mAccounts;
    private ListScrollPositionManager mListScrollPositionManager;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private ListView mListView;

    protected ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(android.R.id.list);
        }
        return mListView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_listview);
        mListScrollPositionManager = new ListScrollPositionManager(getListView(), false);
        setTitle(getString(R.string.blogs_visibility));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // swipe to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (SwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        new UpdateBlogTask(getApplicationContext()).execute();
                    }
                });

        // Load accounts and update from server
        loadAccounts();

        // Refresh blog list if network is available and activity really starts
        if (NetworkUtils.isNetworkAvailable(this) && savedInstanceState == null) {
            refreshBlogs();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_blogs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_show_all:
                selectAll();
                return true;
            case R.id.menu_hide_all:
                deselectAll();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectAll() {
        for (Map<String, Object> item : mAccounts) {
            item.put("isHidden", false);
        }
        WordPress.wpDB.setAllDotComAccountsVisibility(true);
        ((BlogsAdapter) getListView().getAdapter()).notifyDataSetChanged();
    }

    private void deselectAll() {
        for (Map<String, Object> item : mAccounts) {
            item.put("isHidden", true);
        }
        WordPress.wpDB.setAllDotComAccountsVisibility(false);
        ((BlogsAdapter) getListView().getAdapter()).notifyDataSetChanged();
    }

    private void refreshBlogs() {
        mSwipeToRefreshHelper.setRefreshing(true);
        new UpdateBlogTask(getApplicationContext()).execute();
    }

    private void loadAccounts() {
        ListView listView = getListView();
        mAccounts = WordPress.wpDB.getAccountsBy("dotcomFlag=1", new String[]{"isHidden"});
        listView.setAdapter(new BlogsAdapter(this, R.layout.manageblogs_listitem, mAccounts));
    }

    private void setItemChecked(int position, boolean checked) {
        int blogId = MapUtils.getMapInt(mAccounts.get(position), "id");
        WordPress.wpDB.setDotComAccountsVisibility(blogId, checked);
        Map<String, Object> item = mAccounts.get(position);
        item.put("isHidden", checked ? "0" : "1");
    }

    private class BlogsAdapter extends ArrayAdapter<Map<String, Object>> {
        private int mResource;
        private final LayoutInflater mInflater;

        public BlogsAdapter(Context context, int resource, List<Map<String, Object>> objects) {
            super(context, resource, objects);
            mResource = resource;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final BlogItemViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(mResource, parent, false);
                holder = new BlogItemViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (BlogItemViewHolder) convertView.getTag();
            }
            holder.nameView.setText(BlogUtils.getBlogNameFromAccountMap(getItem(position)));
            holder.urlView.setText(BlogUtils.getHostNameFromAccountMap(getItem(position)));
            holder.checkBox.setChecked(!MapUtils.getMapBool(getItem(position), "isHidden"));
            convertView.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                    setItemChecked(position, holder.checkBox.isChecked());
                }
            });
            return convertView;
        }

        private class BlogItemViewHolder {
            private final TextView nameView;
            private final CheckBox checkBox;
            private final TextView urlView;

            BlogItemViewHolder(final View rowView) {
                nameView = (TextView) rowView.findViewById(R.id.blog_name);
                urlView = (TextView) rowView.findViewById(R.id.blog_url);
                checkBox = (CheckBox) rowView.findViewById(R.id.checkbox);
                checkBox.setClickable(false);
            }
        }
    }

    private class UpdateBlogTask extends UpdateBlogListTask {
        public UpdateBlogTask(Context context) {
            super(context);
        }

        @Override
        protected void onPostExecute(final List<Map<String, Object>> userBlogList) {
            if (mErrorMsgId != 0) {
                ToastUtils.showToast(getBaseContext(), mErrorMsgId, ToastUtils.Duration.SHORT);
            }
            mListScrollPositionManager.saveScrollOffset();
            loadAccounts();
            mListScrollPositionManager.restoreScrollOffset();
            mSwipeToRefreshHelper.setRefreshing(false);
        }
    }
}
