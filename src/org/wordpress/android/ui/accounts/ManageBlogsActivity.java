package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;

public class ManageBlogsActivity extends SherlockListActivity {
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
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(this, (PullToRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        new SetupBlogTask().execute();
                    }
                });

        // Load accounts and update from server
        loadAccounts();
        refreshBlogs();
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
        MenuInflater inflater = getSupportMenuInflater();
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
        new SetupBlogTask().execute();
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
            String name = MapUtils.getMapStr(getItem(position), "blogName");
            if (name.trim().length() == 0) {
                name = MapUtils.getMapStr(getItem(position), "url");
                name = StringUtils.getHost(name);
            }
            nameView.setText(name);
            nameView.setChecked(!MapUtils.getMapBool(getItem(position), "isHidden"));
            return rowView;
        }
    }

    private class SetupBlogTask extends AsyncTask<Void, Void, List<Map<String, Object>>> {
        private SetupBlog mSetupBlog;
        private int mErrorMsgId;

        @Override
        protected void onPreExecute() {
            mSetupBlog = new SetupBlog();
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
            mSetupBlog.setUsername(username);
            mSetupBlog.setPassword(password);
        }

        @Override
        protected List<Map<String, Object>> doInBackground(Void... args) {
            List<Map<String, Object>> userBlogList = mSetupBlog.getBlogList();
            mErrorMsgId = mSetupBlog.getErrorMsgId();
            if (userBlogList != null) {
                mSetupBlog.syncBlogs(getApplicationContext(), userBlogList);
            }
            return userBlogList;
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
}