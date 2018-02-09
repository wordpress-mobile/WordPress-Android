package org.wordpress.android.ui.plugins;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
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
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserViewModel extends AndroidViewModel {
    enum PluginListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    @Inject Dispatcher mDispatcher;
    @Inject PluginStore mPluginStore;

    private String mSearchQuery;
    private SiteModel mSite;

    private MutableLiveData<PluginListStatus> mNewPluginsListStatus;
    private MutableLiveData<PluginListStatus> mPopularPluginsListStatus;
    private MutableLiveData<PluginListStatus> mSitePluginsListStatus;
    private MutableLiveData<PluginListStatus> mSearchPluginsListStatus;

    private MutableLiveData<List<WPOrgPluginModel>> mNewPlugins;
    private MutableLiveData<List<WPOrgPluginModel>> mPopularPlugins;
    private MutableLiveData<List<SitePluginModel>> mSitePlugins;
    private MutableLiveData<List<WPOrgPluginModel>> mSearchResults;

    public PluginBrowserViewModel(@NonNull Application application) {
        super(application);

        ((WordPress) application).component().inject(this);
        mDispatcher.register(this);

        mSitePlugins = new MutableLiveData<>();
        mNewPlugins = new MutableLiveData<>();
        mPopularPlugins = new MutableLiveData<>();
        mSearchResults = new MutableLiveData<>();

        mNewPluginsListStatus = new MutableLiveData<>();
        mPopularPluginsListStatus = new MutableLiveData<>();
        mSitePluginsListStatus = new MutableLiveData<>();
        mSearchPluginsListStatus = new MutableLiveData<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDispatcher.unregister(this);
    }

    void start() {
        reloadAllPluginsFromStore();

        fetchPlugins(PluginBrowserActivity.PluginListType.SITE, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.POPULAR, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.NEW, false);
    }

    // Site & WPOrg plugin management

    WPOrgPluginModel getWPOrgPluginForSitePlugin(SitePluginModel sitePlugin) {
        if (sitePlugin == null) {
            return null;
        }
        return PluginUtils.getWPOrgPlugin(mPluginStore, sitePlugin);
    }

    SitePluginModel getSitePluginFromSlug(String slug) {
        return mPluginStore.getSitePluginBySlug(getSite(), slug);
    }

    void reloadAllPluginsFromStore() {
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

    void fetchPlugins(@NonNull PluginBrowserActivity.PluginListType listType, boolean loadMore) {
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

    void fetchWPOrgPlugin(String slug) {
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
    }

    void loadMore(PluginBrowserActivity.PluginListType listType) {
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
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while searching the plugin directory");
            mSearchPluginsListStatus.setValue(PluginListStatus.ERROR);
            return;
        }
        setSearchResults(event.searchTerm, event.plugins);
        mSearchPluginsListStatus.setValue(PluginListStatus.DONE);
    }

    // Search

    void clearSearchResults() {
        mSearchResults.setValue(new ArrayList<WPOrgPluginModel>());
    }

    private void setSearchResults(String searchQuery, List<WPOrgPluginModel> searchResults) {
        if (mSearchQuery.equalsIgnoreCase(searchQuery)) {
            mSearchResults.setValue(searchResults);
        }
    }

    boolean shouldShowEmptySearchResultsView() {
        if (TextUtils.isEmpty(mSearchQuery)) {
            return false;
        }
        if (mSearchPluginsListStatus.getValue() != PluginListStatus.DONE
                && mSearchPluginsListStatus.getValue() != PluginListStatus.ERROR) {
            return false;
        }
        return getSearchResults().getValue() == null || getSearchResults().getValue().size() == 0;
    }

    // Simple Getters & Setters

    SiteModel getSite() {
        return mSite;
    }

    void setSite(SiteModel site) {
        mSite = site;
    }

    String getSearchQuery() {
        return mSearchQuery;
    }

    void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
    }

    LiveData<List<SitePluginModel>> getSitePlugins() {
        return mSitePlugins;
    }

    boolean isSitePluginsEmpty() {
        return getSitePlugins().getValue() == null || getSitePlugins().getValue().size() == 0;
    }

    LiveData<List<WPOrgPluginModel>> getNewPlugins() {
        return mNewPlugins;
    }

    LiveData<List<WPOrgPluginModel>> getPopularPlugins() {
        return mPopularPlugins;
    }

    LiveData<List<WPOrgPluginModel>> getSearchResults() {
        return mSearchResults;
    }

    LiveData<PluginListStatus> getNewPluginsListStatus() {
        return mNewPluginsListStatus;
    }

    LiveData<PluginListStatus> getPopularPluginsListStatus() {
        return mPopularPluginsListStatus;
    }

    LiveData<PluginListStatus> getSitePluginsListStatus() {
        return mSitePluginsListStatus;
    }

    LiveData<PluginListStatus> getSearchPluginsListStatus() {
        return mSearchPluginsListStatus;
    }

    List<?> getPluginsForListType(PluginBrowserActivity.PluginListType listType) {
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

    private boolean isLoadingMorePlugins(PluginBrowserActivity.PluginListType listType) {
        if (listType == PluginBrowserActivity.PluginListType.NEW) {
            return getNewPluginsListStatus().getValue() == PluginListStatus.LOADING_MORE;
        } else if (listType == PluginBrowserActivity.PluginListType.POPULAR) {
            return getPopularPluginsListStatus().getValue() == PluginListStatus.LOADING_MORE;
        }
        return false;
    }
}
