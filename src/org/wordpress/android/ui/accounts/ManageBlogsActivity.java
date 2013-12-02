package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.List;
import java.util.Map;

public class ManageBlogsActivity extends SherlockListActivity {
    private List<Map<String, Object>> mAccounts;
    private MenuItem mRefreshMenuItem;
    private boolean mIsRefreshing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.blogs_visibility));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        loadAccounts();
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
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        refreshBlogs();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_refresh:
                refreshBlogs();
                return true;
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
        for (int i = 0; i < mAccounts.size(); ++i) {
            setItemChecked(i, true);
        }
    }

    private void deselectAll() {
        // force one item selected
        setItemChecked(0, true);
        for (int i = 1; i < mAccounts.size(); ++i) {
            setItemChecked(i, false);
        }
    }

    private void startAnimatingRefreshButton() {
        if (mRefreshMenuItem != null && !mIsRefreshing) {
            mIsRefreshing = true;
            LayoutInflater inflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(
                    getResources().getLayout(R.layout.menu_refresh_view), null);
            RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setInterpolator(new LinearInterpolator());
            anim.setRepeatCount(Animation.INFINITE);
            anim.setDuration(1400);
            iv.startAnimation(anim);
            mRefreshMenuItem.setActionView(iv);
        }
    }

    private void stopAnimatingRefreshButton() {
        mIsRefreshing = false;
        if (mRefreshMenuItem != null) {
            if (mRefreshMenuItem.getActionView() == null) {
                return ;
            }
            mRefreshMenuItem.getActionView().clearAnimation();
            mRefreshMenuItem.setActionView(null);
        }
    }

    private void refreshBlogs() {
        if (!mIsRefreshing) {
            startAnimatingRefreshButton();
            new SetupBlogTask().execute();
        }
    }

    private void loadAccounts() {
        ListView listView = getListView();
        mAccounts = WordPress.wpDB.getAccountsBy("dotcomFlag=1", new String[] {"isHidden"});
        listView.setAdapter(new BlogsAdapter(this, R.layout.manageblogs_listitem, mAccounts));
    }

    private void setItemChecked(int position, boolean checked) {
        int blogId = MapUtils.getMapInt(mAccounts.get(position), "id");
        Blog blog = WordPress.getBlog(blogId);
        if (blog == null) {
            Log.e(WordPress.TAG, "Error, blog id not found: " + blogId);
            return ;
        }
        blog.setHidden(!checked);
        blog.save();
        Map<String, Object> item = mAccounts.get(position);
        item.put("isHidden", checked ? "0" : "1");
        ((BlogsAdapter)getListView().getAdapter()).notifyDataSetChanged();
    }

    private int blogShownCount() {
        int nChecked = 0;
        for (Map<String, Object> account : mAccounts) {
            if (!MapUtils.getMapBool(account, "isHidden")) {
                nChecked += 1;
            }
        }
        return nChecked;
    }

    private class BlogsAdapter extends ArrayAdapter<Map<String, Object>> {
        private int mResource;

        public BlogsAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
            mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(mResource, parent, false);
            CheckedTextView nameView = (CheckedTextView) rowView.findViewById(R.id.blog_name);
            nameView.setText(MapUtils.getMapStr(getItem(position), "blogName"));
            nameView.setChecked(!MapUtils.getMapBool(getItem(position), "isHidden"));
            if (blogShownCount() == 1 && nameView.isChecked()) {
                nameView.setEnabled(false);
                nameView.setClickable(true);
            } else {
                nameView.setEnabled(true);
                nameView.setClickable(false);
            }
            return rowView;
        }
    }

    private class SetupBlogTask extends AsyncTask<Void, Void, List<Object>> {
        private SetupBlog mSetupBlog;
        private int mErrorMsgId;

        @Override
        protected void onPreExecute() {
            mSetupBlog = new SetupBlog();
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext());
            String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            String password = WordPressDB.decryptPassword(settings.
                    getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
            mSetupBlog.setUsername(username);
            mSetupBlog.setPassword(password);
        }

        @Override
        protected List doInBackground(Void... args) {
            List userBlogList = mSetupBlog.getBlogList();
            mErrorMsgId = mSetupBlog.getErrorMsgId();
            if (userBlogList != null) {
                // Add all blogs
                SparseBooleanArray allBlogs = new SparseBooleanArray();
                for (int i = 0; i < userBlogList.size(); i++) {
                    allBlogs.put(i, true);
                }
                mSetupBlog.addBlogs(userBlogList, allBlogs);
            }
            return userBlogList;
        }

        @Override
        protected void onPostExecute(final List<Object> userBlogList) {
            if (mErrorMsgId != 0) {
                ToastUtils.showToast(getBaseContext(), mErrorMsgId, ToastUtils.Duration.SHORT);
            }
            loadAccounts();
            stopAnimatingRefreshButton();
        }
    }
}