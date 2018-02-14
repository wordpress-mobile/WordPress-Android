package org.wordpress.android.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
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
import org.wordpress.android.ui.plugins.PluginBrowserActivity;
import org.wordpress.android.ui.plugins.PluginUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserViewModel extends ViewModel {
    public enum PluginListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    private final Dispatcher mDispatcher;
    private final PluginStore mPluginStore;

    private String mSearchQuery;
    private SiteModel mSite;

    private final Handler mHandler;

    private MutableLiveData<PluginListStatus> mNewPluginsListStatus;
    private MutableLiveData<PluginListStatus> mPopularPluginsListStatus;
    private MutableLiveData<PluginListStatus> mSitePluginsListStatus;
    private MutableLiveData<PluginListStatus> mSearchPluginsListStatus;

    private MutableLiveData<List<WPOrgPluginModel>> mNewPlugins;
    private MutableLiveData<List<WPOrgPluginModel>> mPopularPlugins;
    private MutableLiveData<List<SitePluginModel>> mSitePlugins;
    private MutableLiveData<List<WPOrgPluginModel>> mSearchResults;

    private MutableLiveData<String> mLastUpdatedWpOrgPluginSlug;

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
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDispatcher.unregister(this);
    }

    public void start() {
        reloadAllPluginsFromStore();

        fetchPlugins(PluginBrowserActivity.PluginListType.SITE, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.POPULAR, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.NEW, false);
    }

    // Site & WPOrg plugin management

    public WPOrgPluginModel getWPOrgPluginForSitePlugin(SitePluginModel sitePlugin) {
        if (sitePlugin == null) {
            return null;
        }
        return PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
    }

    public SitePluginModel getSitePluginFromSlug(String slug) {
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

    // Network Requests

    private void fetchPlugins(@NonNull PluginBrowserActivity.PluginListType listType, boolean loadMore) {
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

    private boolean shouldFetchPlugins(PluginBrowserActivity.PluginListType listType, boolean loadMore) {
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

    public void fetchWPOrgPlugin(String slug) {
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
    }

    public void loadMore(PluginBrowserActivity.PluginListType listType) {
        fetchPlugins(listType, true);
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
        if (!mSearchQuery.equals(event.searchTerm)) {
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

    private void submitSearch(@Nullable final String query, boolean delayed) {
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
                fetchPlugins(PluginBrowserActivity.PluginListType.SEARCH, false);
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

    public List<?> getPluginsForListType(PluginBrowserActivity.PluginListType listType) {
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
