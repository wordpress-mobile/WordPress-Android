package org.wordpress.android.ui.main;

import android.content.Context;
import android.os.AsyncTask;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SitePickerSearchAdapter extends SitePickerAdapter {
    private String mLastSearch;
    private SiteList mAllSites;

    public SitePickerSearchAdapter(Context context, int currentLocalBlogId, String lastSearch) {
        super(context, currentLocalBlogId);

        if (lastSearch == null) {
            mLastSearch = "";
        } else {
            mLastSearch = lastSearch;
        }

        mAllSites = new SiteList();
    }

    public void searchSites(String searchText) {
        mSites = filteredSitesByText(mAllSites, searchText);

        notifyDataSetChanged();
    }

    @Override
    public void loadSites() {
        if (mIsSearchTaskRunning) {
            AppLog.w(AppLog.T.UTILS, "site picker > already loading sites");
        } else {
            new LoadSearchSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private SiteList filteredSitesByText(SiteList sites, String searchText) {
        SiteList filteredSiteList = new SiteList();
        mLastSearch = searchText.toLowerCase();

        for (int i = 0; i < sites.size(); i++) {
            SiteRecord record = sites.get(i);
            String siteNameLowerCase = record.blogName.toLowerCase();
            String hostNameLowerCase = record.hostName.toLowerCase();

            if (siteNameLowerCase.contains(mLastSearch) || hostNameLowerCase.contains(mLastSearch)) {
                filteredSiteList.add(record);
            }
        }

        return filteredSiteList;
    }

    private boolean mIsSearchTaskRunning;

    private class LoadSearchSitesTask extends AsyncTask<Void, Void, SiteList> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsSearchTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsSearchTaskRunning = false;
        }

        @Override
        protected SiteList doInBackground(Void... params) {
            List<Map<String, Object>> blogs = WordPress.wpDB.getAllBlogs();

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
                mAllSites = (SiteList) sites.clone();
                mSites = filteredSitesByText(sites, mLastSearch);
                notifyDataSetChanged();
            }
            mIsSearchTaskRunning = false;
        }
    }
}
