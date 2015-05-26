package org.wordpress.android.ui.main;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SitePickerSearchAdapter extends SitePickerAdapter {
    SiteList mAllSites;

    public SitePickerSearchAdapter(Context context, int currentLocalBlogId) {
        super(context, currentLocalBlogId);
        mIsSearchMode = true;
        mAllSites = null;
    }

    @Override
    void loadSites() {
        if (mIsTaskRunning) {
            AppLog.w(AppLog.T.UTILS, "site picker > already loading sites");
        } else {
            new SearchLoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    void search(String s) {
        if (!s.isEmpty()) {
            mSites = new SiteList();

            for (int i = 0; i < mAllSites.size(); i++) {
                SiteRecord record = mAllSites.get(i);
                String siteName = record.blogName;
                String siteUrl = record.hostName;

                if (siteName.toLowerCase().contains(s.toLowerCase()) || siteUrl.toLowerCase().contains(s.toLowerCase())) {
                    mSites.add(record);
                }
            }
        }

        notifyDataSetChanged();
    }

    private class SearchLoadSitesTask extends AsyncTask<Void, Void, SiteList> {
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
            String[] extraFields = {"isHidden", "dotcomFlag"};

            blogs = WordPress.wpDB.getBlogsBy(null, extraFields);

            SiteList sites = new SiteList(blogs);

            // sort by blog/host
            Collections.sort(sites, new Comparator<SiteRecord>() {
                public int compare(SiteRecord site1, SiteRecord site2) {
                    return site1.getBlogNameOrHostName().compareToIgnoreCase(site2.getBlogNameOrHostName());
                }
            });

            return sites;
        }

        @Override
        protected void onPostExecute(SiteList sites) {
            if (mSites == null || !mSites.isSameList(sites)) {
                mSites = sites;
                mAllSites = (SiteList) sites.clone();
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
        }
    }
}
