package org.wordpress.android.ui.accounts;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ptr.PullToRefreshHelper;
import org.wordpress.android.util.ptr.PullToRefreshHelper.RefreshListener;

import java.util.List;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

public class ManageBlogsActivity extends ListActivity {
    private List<Map<String, Object>> mAccounts;
    private static boolean mIsRefreshing;
    private ListScrollPositionManager mListScrollPositionManager;
    private PullToRefreshHelper mPullToRefreshHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_listview);
        mListScrollPositionManager = new ListScrollPositionManager(getListView(), false);
        setTitle(getString(R.string.blogs_visibility));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(this, (PullToRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mPullToRefreshHelper.setRefreshing(false);
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        CheckedTextView checkedView = (CheckedTextView) v;
        checkedView.setChecked(!checkedView.isChecked());
        setItemChecked(position, checkedView.isChecked());
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
            case R.id.menu_refresh:
                WordPress.sendLocalBroadcast(this, PullToRefreshHelper.BROADCAST_ACTION_REFRESH_MENU_PRESSED);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectAll() {
        for (Map<String, Object> item : mAccounts) {
            item.put("isHidden", false);
        }
        WordPress.wpDB.setAllDotComAccountsVisibility(true);
        ((BlogsAdapter)getListView().getAdapter()).notifyDataSetChanged();
    }

    private void deselectAll() {
        for (Map<String, Object> item : mAccounts) {
            item.put("isHidden", true);
        }
        WordPress.wpDB.setAllDotComAccountsVisibility(false);
        ((BlogsAdapter)getListView().getAdapter()).notifyDataSetChanged();
    }

    private void refreshBlogs() {
        mPullToRefreshHelper.setRefreshing(true);
        new UpdateBlogTask(getApplicationContext()).execute();
    }

    private void loadAccounts() {
        ListView listView = getListView();
        mAccounts = WordPress.wpDB.getAccountsBy("dotcomFlag=1", new String[] {"isHidden"});
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

        public BlogsAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
            mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(mResource, parent, false);
            CheckedTextView nameView = (CheckedTextView) rowView.findViewById(R.id.blog_name);
            nameView.setText(BlogUtils.getBlogNameFromAccountMap(getItem(position)));
            nameView.setChecked(!MapUtils.getMapBool(getItem(position), "isHidden"));
            return rowView;
        }
    }

    private class UpdateBlogTask extends SetupBlogTask {
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
            mPullToRefreshHelper.setRefreshing(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPullToRefreshHelper.unregisterReceiver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPullToRefreshHelper.registerReceiver(this);
    }
}