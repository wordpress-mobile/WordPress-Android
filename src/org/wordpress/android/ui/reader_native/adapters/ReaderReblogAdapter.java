package org.wordpress.android.ui.reader_native.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.SysUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by nbradbury on 9/19/13.
 * adapter which display list of blogs (accounts) for user to choose from when reblogging
 */
public class ReaderReblogAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    SimpleAccountList mAccounts = new SimpleAccountList();

    public ReaderReblogAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loadAccounts();
    }

    @SuppressLint("NewApi")
    private void loadAccounts() {
        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadAccountsTask().execute();
        }
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public Object getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mAccounts.get(position).blogId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = mInflater.inflate(R.layout.reader_reblog_item, null);
        TextView txtBlogName = (TextView) view.findViewById(R.id.text);
        txtBlogName.setText(mAccounts.get(position).blogName);
        return view;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        view = mInflater.inflate(R.layout.reader_reblog_dropdown_item, null);
        TextView txtBlogName = (TextView) view.findViewById(R.id.text);
        txtBlogName.setText(mAccounts.get(position).blogName);
        return view;
    }

    private class SimpleAccountItem {
        int blogId;
        String blogName;

        private SimpleAccountItem(int blogId, String blogName) {
            this.blogId = blogId;
            this.blogName = blogName;
        }
    }

    private class SimpleAccountList extends ArrayList<SimpleAccountItem> {}

    /*
     * AsyncTask to retrieve list of blogs (accounts) from db
     */
    private boolean mIsTaskRunning;
    private class LoadAccountsTask extends AsyncTask<Void, Void, Boolean> {
        SimpleAccountList tmpAccounts = new SimpleAccountList();

        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            List<Map<String, Object>> accounts = WordPress.wpDB.getAccounts();
            if (accounts == null || accounts.size() == 0)
                return false;

            Blog blog = WordPress.getCurrentBlog();
            int currentBlogId = (blog != null ? blog.getBlogId() : 0);

            Iterator<Map<String, Object>> it = accounts.iterator();
            while (it.hasNext()) {
                Map<String, Object> curHash = it.next();

                int blogId = (Integer) curHash.get("blogId");
                String blogName = StringUtils.unescapeHTML(curHash.get("blogName").toString());
                if (TextUtils.isEmpty(blogName))
                    blogName = curHash.get("url").toString();

                SimpleAccountItem item = new SimpleAccountItem(blogId, blogName);

                // if this is the current blog, insert it at the top so it's automatically selected
                if (tmpAccounts.size() > 0 && blogId == currentBlogId) {
                    tmpAccounts.add(0, item);
                } else {
                    tmpAccounts.add(item);
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mAccounts = (SimpleAccountList) tmpAccounts.clone();
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
        }
    }
}