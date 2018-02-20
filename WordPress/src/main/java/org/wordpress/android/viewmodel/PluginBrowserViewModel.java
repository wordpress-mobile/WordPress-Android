package org.wordpress.android.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserViewModel extends ViewModel {
    public enum PluginListType {
        SITE,
        POPULAR,
        NEW,
        SEARCH
    }

    public enum PluginListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    private static final String KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY";
    private static final String KEY_TITLE = "KEY_TITLE";

    private final Dispatcher mDispatcher;
    private final PluginStore mPluginStore;

    private boolean mIsStarted = false;
    private String mSearchQuery;
    private SiteModel mSite;

    private final Handler mHandler;

    private final MutableLiveData<PluginListStatus> mNewPluginsListStatus;
    private final MutableLiveData<PluginListStatus> mPopularPluginsListStatus;
    private final MutableLiveData<PluginListStatus> mSitePluginsListStatus;
    private final MutableLiveData<PluginListStatus> mSearchPluginsListStatus;

    private final MutableLiveData<List<WPOrgPluginModel>> mNewPlugins;
    private final MutableLiveData<List<WPOrgPluginModel>> mPopularPlugins;
    private final MutableLiveData<List<SitePluginModel>> mSitePlugins;
    private final MutableLiveData<List<WPOrgPluginModel>> mSearchResults;

    private final MutableLiveData<String> mLastUpdatedWpOrgPluginSlug;
    private final MutableLiveData<String> mTitle;

    @SuppressWarnings("WeakerAccess")
    @Inject
    public PluginBrowserViewModel(@NonNull Dispatcher dispatcher, @NonNull PluginStore pluginStore) {
        super();
        mDispatcher = dispatcher;
        mPluginStore = pluginStore;

        mDispatcher.register(this);

        mHandler = new Handler();

        mSitePlugins = new MutableLiveData<>();
        mNewPlugins = new MutableLiveData<>();
        mPopularPlugins = new MutableLiveData<>();
        mSearchResults = new MutableLiveData<>();

        mNewPluginsListStatus = new MutableLiveData<>();
        mPopularPluginsListStatus = new MutableLiveData<>();
        mSitePluginsListStatus = new MutableLiveData<>();
        mSearchPluginsListStatus = new MutableLiveData<>();
        mLastUpdatedWpOrgPluginSlug = new MutableLiveData<>();
        mTitle = new MutableLiveData<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDispatcher.unregister(this);
    }

    public void writeToBundle(@NonNull Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        outState.putString(KEY_TITLE, mTitle.getValue());
    }

    public void readFromBundle(@NonNull Bundle savedInstanceState) {
        if (mIsStarted) {
            // This was called due to a config change where the data survived, we don't need to
            // read from the bundle
            return;
        }
        mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
        setTitle(savedInstanceState.getString(KEY_TITLE));
    }

    public void start() {
        if (mIsStarted) {
            return;
        }
        reloadAllPluginsFromStore();

        fetchPlugins(PluginListType.SITE, false);
        fetchPlugins(PluginListType.POPULAR, false);
        fetchPlugins(PluginListType.NEW, false);
        // If activity is recreated we need to re-search
        if (shouldSearch()) {
            fetchPlugins(PluginListType.SEARCH, false);
        }

        mIsStarted = true;
    }

    // Site & WPOrg plugin management

    public WPOrgPluginModel getWPOrgPluginForSitePluginAndFetchIfNecessary(SitePluginModel sitePlugin) {
        if (sitePlugin == null) {
            return null;
        }
        WPOrgPluginModel wpOrgPlugin = PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
        if (wpOrgPlugin == null) {
            fetchWPOrgPlugin(sitePlugin.getSlug());
        }
        return wpOrgPlugin;
    }

    public SitePluginModel getSitePluginFromSlug(String slug) {
        List<SitePluginModel> sitePlugins = getSitePlugins().getValue();
        if (sitePlugins != null) {
            // TODO: if we ever add caching to PluginStore, remove this
            for (SitePluginModel plugin : sitePlugins) {
                if (plugin.getSlug().equals(slug)) {
                    return plugin;
                }
            }
        }
        return mPluginStore.getSitePluginBySlug(getSite(), slug);
    }

    public void reloadAllPluginsFromStore() {
        reloadSitePlugins();
        reloadPopularPlugins();
        reloadNewPlugins();
    }

    private void reloadSitePlugins() {
        List<SitePluginModel> sitePlugins = mPluginStore.getSitePlugins(getSite());
        mSitePlugins.setValue(sitePlugins);
    }

    private void reloadNewPlugins() {
        mNewPlugins.setValue(mPluginStore.getPluginDirectory(PluginDirectoryType.NEW));
    }

    private void reloadPopularPlugins() {
        mPopularPlugins.setValue(mPluginStore.getPluginDirectory(PluginDirectoryType.POPULAR));
    }

    // Pull to refresh

    public void pullToRefresh(@NonNull PluginListType pluginListType) {
        fetchPlugins(pluginListType, false);
    }

    // Network Requests

    private void fetchPlugins(@NonNull PluginListType listType, boolean loadMore) {
        if (!shouldFetchPlugins(listType, loadMore)) {
            return;
        }
        switch (listType) {
            case SITE:
                mSitePluginsListStatus.setValue(PluginListStatus.FETCHING);
                mDispatcher.dispatch(PluginActionBuilder.newFetchSitePluginsAction(getSite()));
                break;
            case POPULAR:
                mPopularPluginsListStatus.setValue(loadMore ? PluginListStatus.LOADING_MORE : PluginListStatus.FETCHING);
                PluginStore.FetchPluginDirectoryPayload popularPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload));
                break;
            case NEW:
                mNewPluginsListStatus.setValue(loadMore ? PluginListStatus.LOADING_MORE : PluginListStatus.FETCHING);
                PluginStore.FetchPluginDirectoryPayload newPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.NEW, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload));
                break;
            case SEARCH:
                mSearchPluginsListStatus.setValue(PluginListStatus.FETCHING);
                PluginStore.SearchPluginDirectoryPayload searchPayload =
                        new PluginStore.SearchPluginDirectoryPayload(getSearchQuery(), 1);
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload));
                break;
        }
    }

    private boolean shouldFetchPlugins(PluginListType listType, boolean loadMore) {
        if (loadMore && !isLoadMoreEnabled(listType)) {
            // If we are trying to load more and it's not allowed
            return false;
        }
        switch (listType) {
            case SITE:
                if (getSitePluginsListStatus().getValue() == PluginListStatus.FETCHING) {
                    // already fetching
                    return false;
                }
                break;
            case POPULAR:
                if (!loadMore && getPopularPluginsListStatus().getValue() == PluginListStatus.FETCHING) {
                    // already fetching first page
                    return false;
                }
                if (loadMore && getPopularPluginsListStatus().getValue() != PluginListStatus.CAN_LOAD_MORE) {
                    // We might be fetching the first page, loading more or done fetching
                    return false;
                }
                break;
            case NEW:
                if (!loadMore && getNewPluginsListStatus().getValue() == PluginListStatus.FETCHING) {
                    // already fetching first page
                    return false;
                }
                if (loadMore && getNewPluginsListStatus().getValue() != PluginListStatus.CAN_LOAD_MORE) {
                    // We might be fetching the first page, loading more or done fetching
                    return false;
                }
                break;
            case SEARCH:
                // Since search query might have been changed, we should always fetch
                return true;
        }
        return true;
    }

    private void fetchWPOrgPlugin(String slug) {
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
    }

    public void loadMore(PluginListType listType) {
        if (isLoadMoreEnabled(listType)) {
            fetchPlugins(listType, true);
        }
    }

    private boolean isLoadMoreEnabled(PluginListType listType) {
        // We don't use pagination for Site plugins or Search results
        return listType != PluginListType.SITE && listType != PluginListType.SEARCH;
    }

    // Network Callbacks

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginsFetched(PluginStore.OnSitePluginsFetched event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching site plugins with type: " + event.error.type);
            mSitePluginsListStatus.setValue(PluginListStatus.ERROR);
            return;
        }
        mSitePluginsListStatus.setValue(PluginListStatus.DONE);
        reloadSitePlugins();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }

        if (!TextUtils.isEmpty(event.pluginSlug)) {
            mLastUpdatedWpOrgPluginSlug.setValue(event.pluginSlug);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectoryFetched(PluginStore.OnPluginDirectoryFetched event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the plugin directory: " + event.type);
            if (event.type == PluginDirectoryType.NEW) {
                mNewPluginsListStatus.setValue(PluginListStatus.ERROR);
            } else if (event.type == PluginDirectoryType.POPULAR) {
                mPopularPluginsListStatus.setValue(PluginListStatus.ERROR);
            }
            return;
        }
        PluginListStatus listStatus = event.canLoadMore ? PluginListStatus.CAN_LOAD_MORE : PluginListStatus.DONE;
        switch (event.type) {
            case NEW:
                reloadNewPlugins();
                mNewPluginsListStatus.setValue(listStatus);
                break;
            case POPULAR:
                reloadPopularPlugins();
                mPopularPluginsListStatus.setValue(listStatus);
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectorySearched(PluginStore.OnPluginDirectorySearched event) {
        if (mSearchQuery == null || !mSearchQuery.equals(event.searchTerm)) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while searching the plugin directory");
            mSearchPluginsListStatus.setValue(PluginListStatus.ERROR);
            return;
        }
        mSearchResults.setValue(event.plugins);
        mSearchPluginsListStatus.setValue(PluginListStatus.DONE);
    }

    // Search

    public void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;

        // Don't delay if the searchQuery is empty
        submitSearch(searchQuery, !TextUtils.isEmpty(searchQuery));
    }

    private boolean shouldSearch() {
        // We need at least 2 characters to be able to search plugins
        return getSearchQuery() != null && getSearchQuery().length() > 1;
    }

    // Make the method protected to avoid synthetic accessor methods
    @SuppressWarnings("WeakerAccess")
    protected void submitSearch(@Nullable final String query, boolean delayed) {
        if (delayed) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (StringUtils.equals(query, getSearchQuery())) {
                        submitSearch(query, false);
                    }
                }
            }, 250);
        } else {
            clearSearchResults();

            if (shouldSearch()) {
                fetchPlugins(PluginListType.SEARCH, false);
            } else {
                // Due to the query being changed after the last fetch, the status won't ever be updated, so we need
                // to manually do it. Consider the following case:
                // 1. Search the plugins for "contact" which will change the status to FETCHING
                // 2. Before the fetch completes delete the text
                // 3. In `onPluginDirectorySearched` the result will be ignored, because the query changed, but it won't
                // be triggered again, because another fetch didn't happen (due to query being empty)
                // 4. The status will be stuck in FETCHING until another search occurs. This following reset fixes the
                // problem.
                mSearchPluginsListStatus.setValue(PluginListStatus.DONE);
            }
        }
    }

    private void clearSearchResults() {
        mSearchResults.setValue(new ArrayList<WPOrgPluginModel>());
    }

    public boolean shouldShowEmptySearchResultsView() {
        // Search query is less than 2 characters
        if (!shouldSearch()) {
            return false;
        }
        if (mSearchPluginsListStatus.getValue() != PluginListStatus.DONE
                && mSearchPluginsListStatus.getValue() != PluginListStatus.ERROR) {
            return false;
        }
        return getSearchResults().getValue() == null || getSearchResults().getValue().size() == 0;
    }

    // Simple Getters & Setters

    public SiteModel getSite() {
        return mSite;
    }

    public void setSite(SiteModel site) {
        mSite = site;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    public LiveData<List<SitePluginModel>> getSitePlugins() {
        return mSitePlugins;
    }

    public boolean isSitePluginsEmpty() {
        return getSitePlugins().getValue() == null || getSitePlugins().getValue().size() == 0;
    }

    public LiveData<List<WPOrgPluginModel>> getNewPlugins() {
        return mNewPlugins;
    }

    public LiveData<List<WPOrgPluginModel>> getPopularPlugins() {
        return mPopularPlugins;
    }

    public LiveData<List<WPOrgPluginModel>> getSearchResults() {
        return mSearchResults;
    }

    public LiveData<PluginListStatus> getNewPluginsListStatus() {
        return mNewPluginsListStatus;
    }

    public LiveData<PluginListStatus> getPopularPluginsListStatus() {
        return mPopularPluginsListStatus;
    }

    public LiveData<PluginListStatus> getSitePluginsListStatus() {
        return mSitePluginsListStatus;
    }

    public LiveData<PluginListStatus> getSearchPluginsListStatus() {
        return mSearchPluginsListStatus;
    }

    public LiveData<String> getLastUpdatedWpOrgPluginSlug() {
        return mLastUpdatedWpOrgPluginSlug;
    }

    public void setTitle(String title) {
        mTitle.setValue(title);
    }

    public LiveData<String> getTitle() {
        return mTitle;
    }

    public List<?> getPluginsForListType(PluginListType listType) {
        switch (listType) {
            case SITE:
                return getSitePlugins().getValue();
            case POPULAR:
                return getPopularPlugins().getValue();
            case NEW:
                return getNewPlugins().getValue();
            case SEARCH:
                return getSearchResults().getValue();
        }
        return null;
    }
}
