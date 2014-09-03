package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.ReaderInterfaces.DataLoadedListener;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.DisplayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * adapter which displays list of blogs (accounts) for user to choose from when reblogging
 */
public class ReaderReblogAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final DataLoadedListener mDataLoadedListener;
    private final long mExcludeBlogId;
    private final boolean mIsLandscape;
    private SimpleAccountList mAccounts = new SimpleAccountList();

    public ReaderReblogAdapter(Context context,
                               long excludeBlogId,
                               DataLoadedListener dataLoadedListener) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mExcludeBlogId = excludeBlogId;
        mDataLoadedListener = dataLoadedListener;
        mIsLandscape = DisplayUtils.isLandscape(context);
        loadAccounts();
    }

    private void loadAccounts() {
        new LoadAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public int indexOfBlogId(long blogId) {
        for (int i = 0; i < mAccounts.size(); i++) {
            if (mAccounts.get(i).remoteBlogId == blogId) {
                return i;
            }
        }
        return -1;
    }

    public void reload() {
        clear();
        loadAccounts();
    }

    private void clear() {
        if (mAccounts.size() > 0) {
            mAccounts.clear();
            notifyDataSetChanged();
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
        if (position == -1) {
            return position;
        }
        return mAccounts.get(position).remoteBlogId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReblogHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_actionbar_reblog_item, parent, false);
            holder = new ReblogHolder(convertView, mIsLandscape);
            convertView.setTag(holder);
        } else {
            holder = (ReblogHolder)convertView.getTag();
        }
        holder.text.setText(mAccounts.get(position).blogName);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        final ReblogHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_actionbar_dropdown_item, parent, false);
            holder = new ReblogHolder(convertView, mIsLandscape);
            convertView.setTag(holder);
        } else {
            holder = (ReblogHolder)convertView.getTag();
        }
        holder.text.setText(mAccounts.get(position).blogName);
        return convertView;
    }

    static class ReblogHolder {
        final TextView text;
        ReblogHolder(View view, boolean isLandscape) {
            text = (TextView) view.findViewById(R.id.text);
            if (isLandscape) {
                ((LinearLayout) view).setOrientation(LinearLayout.HORIZONTAL);
            }
        }
    }

    private class SimpleAccountItem {
        final int remoteBlogId;
        final String blogName;

        private SimpleAccountItem(int blogId, String blogName) {
            this.remoteBlogId = blogId;
            this.blogName = blogName;
        }
    }

    private class SimpleAccountList extends ArrayList<SimpleAccountItem> {}

    /*
     * AsyncTask to retrieve list of blogs (accounts) from db
     */
    private class LoadAccountsTask extends AsyncTask<Void, Void, Boolean> {
        final SimpleAccountList tmpAccounts = new SimpleAccountList();

        @Override
        protected Boolean doInBackground(Void... voids) {
            // only .com blogs support reblogging
            List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleDotComAccounts();
            if (accounts == null || accounts.size() == 0) {
                return false;
            }

            int currentRemoteBlogId = WordPress.getCurrentRemoteBlogId();

            for (Map<String, Object> curHash : accounts) {
                int blogId = (Integer) curHash.get("blogId");
                // don't add if this is the blog we're excluding (prevents reblogging to
                // the same blog the post is from)
                if (blogId != mExcludeBlogId) {
                    String blogName =  BlogUtils.getBlogNameFromAccountMap(curHash);
                    SimpleAccountItem item = new SimpleAccountItem(blogId, blogName);

                    // if this is the current blog, insert it at the top so it's automatically selected
                    if (tmpAccounts.size() > 0 && blogId == currentRemoteBlogId) {
                        tmpAccounts.add(0, item);
                    } else {
                        tmpAccounts.add(item);
                    }
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

            if (mDataLoadedListener != null) {
                mDataLoadedListener.onDataLoaded(isEmpty());
            }
        }
    }
}