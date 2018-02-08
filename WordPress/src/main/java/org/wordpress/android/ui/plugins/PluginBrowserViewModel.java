package org.wordpress.android.ui.plugins;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

public class PluginBrowserViewModel extends AndroidViewModel {
    @Inject Dispatcher mDispatcher;
    @Inject PluginStore mPluginStore;

    private String mSearchQuery;
    private SiteModel mSite;

    private boolean mCanLoadMoreNewPlugins = true;
    private boolean mCanLoadMorePopularPlugins = true;

    private MutableLiveData<Boolean> mIsLoadingMoreNewPlugins;
    private MutableLiveData<Boolean> mIsLoadingMorePopularPlugins;

    private final HashMap<String, SitePluginModel> mSitePluginsCache = new HashMap<>();
    private final HashMap<String, WPOrgPluginModel> mWPOrgPluginsForSitePluginsCache = new HashMap<>();

    private MutableLiveData<List<WPOrgPluginModel>> mNewPlugins;
    private MutableLiveData<List<WPOrgPluginModel>> mPopularPlugins;
    private MutableLiveData<List<SitePluginModel>> mSitePlugins;
    private MutableLiveData<List<WPOrgPluginModel>> mSearchResults;

    public PluginBrowserViewModel(@NonNull Application application) {
        super(application);

        ((WordPress) application).component().inject(this);
        mDispatcher.register(this);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mDispatcher.unregister(this);
    }

    SiteModel getSite() {
        return mSite;
    }

    void setSite(SiteModel site) {
        mSite = site;
    }

    // Site & WPOrg plugin management

    MutableLiveData<List<SitePluginModel>> getSitePlugins() {
        if (mSitePlugins == null) {
            mSitePlugins = new MutableLiveData<>();
        }
        return mSitePlugins;
    }

    MutableLiveData<List<WPOrgPluginModel>> getNewPlugins() {
        if (mNewPlugins == null) {
            mNewPlugins = new MutableLiveData<>();
        }
        return mNewPlugins;
    }

    MutableLiveData<List<WPOrgPluginModel>> getPopularPlugins() {
        if (mPopularPlugins == null) {
            mPopularPlugins = new MutableLiveData<>();
        }
        return mPopularPlugins;
    }

    MutableLiveData<List<WPOrgPluginModel>> getSearchResults() {
        if (mSearchResults == null) {
            mSearchResults = new MutableLiveData<>();
        }
        return mSearchResults;
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

    void reloadAndFetchAllPlugins() {
        reloadAllPluginsFromStore();

        fetchPlugins(PluginBrowserActivity.PluginListType.SITE, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.POPULAR, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.NEW, false);
    }

    void reloadAllPluginsFromStore() {
        reloadSitePlugins();
        reloadPopularPlugins();
        reloadNewPlugins();
    }

    private void reloadSitePlugins() {
        List<SitePluginModel> sitePlugins = mPluginStore.getSitePlugins(getSite());
        // Preload the wporg plugins to avoid hitting the DB in onBindViewHolder
        for (SitePluginModel pluginModel : sitePlugins) {
            cacheWPOrgPluginIfNecessary(mPluginStore.getWPOrgPluginBySlug(pluginModel.getSlug()));
        }
        mSitePluginsCache.clear();
        for (SitePluginModel plugin: sitePlugins) {
            mSitePluginsCache.put(plugin.getSlug(), plugin);
        }
        getSitePlugins().setValue(sitePlugins);
    }

    private void reloadNewPlugins() {
        getNewPlugins().setValue(mPluginStore.getPluginDirectory(PluginDirectoryType.NEW));
    }

    private void reloadPopularPlugins() {
        getPopularPlugins().setValue(mPluginStore.getPluginDirectory(PluginDirectoryType.POPULAR));
    }

    private boolean canLoadMorePlugins(PluginBrowserActivity.PluginListType listType) {
        if (listType == PluginBrowserActivity.PluginListType.NEW) {
            return mCanLoadMoreNewPlugins;
        } else if (listType == PluginBrowserActivity.PluginListType.POPULAR) {
            return mCanLoadMorePopularPlugins;
        }
        // site plugins are retrieved all at once so "load more" isn't necessary, search returns
        // the first 50 best matches which we've decided is enough
        return false;
    }

    MutableLiveData<Boolean> getIsLoadingMoreNewPlugins() {
        if (mIsLoadingMoreNewPlugins == null) {
            mIsLoadingMoreNewPlugins = new MutableLiveData<>();
        }
        return mIsLoadingMoreNewPlugins;
    }

    MutableLiveData<Boolean> getIsLoadingMorePopularPlugins() {
        if (mIsLoadingMorePopularPlugins == null) {
            mIsLoadingMorePopularPlugins = new MutableLiveData<>();
        }
        return mIsLoadingMorePopularPlugins;
    }

    private boolean isLoadingMorePlugins(PluginBrowserActivity.PluginListType listType) {
        if (listType == PluginBrowserActivity.PluginListType.NEW) {
            Boolean b = getIsLoadingMoreNewPlugins().getValue();
            return b != null ? b : false;
        } else if (listType == PluginBrowserActivity.PluginListType.POPULAR) {
            Boolean b = getIsLoadingMorePopularPlugins().getValue();
            return b != null ? b : false;
        }
        return false;
    }

    // Network Requests

    void fetchPlugins(@NonNull PluginBrowserActivity.PluginListType pluginType, boolean loadMore) {
        if (loadMore && (!canLoadMorePlugins(pluginType) || isLoadingMorePlugins(pluginType))) {
            // Either we can't load any more plugins or we are already loading more, so ignore
            return;
        }
        switch (pluginType) {
            case SITE:
//                if (mPluginStore.getSitePlugins(mViewModel.getSite()).size() == 0) {
//                    showProgress(true);
//                }
                mDispatcher.dispatch(PluginActionBuilder.newFetchSitePluginsAction(getSite()));
                break;
            case POPULAR:
                getIsLoadingMorePopularPlugins().setValue(loadMore);
                PluginStore.FetchPluginDirectoryPayload popularPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload));
                break;
            case NEW:
                getIsLoadingMoreNewPlugins().setValue(loadMore);
                PluginStore.FetchPluginDirectoryPayload newPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.NEW, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload));
                break;
            case SEARCH:
//                if (mViewModel.getSearchResults().size() == 0) {
//                    showProgress(true);
//                }
                PluginStore.SearchPluginDirectoryPayload searchPayload =
                        new PluginStore.SearchPluginDirectoryPayload(getSearchQuery(), 1);
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload));
                break;
        }
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
        if (!event.isError()) {
            reloadSitePlugins();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (!event.isError()) {
            cacheWPOrgPluginIfNecessary(mPluginStore.getWPOrgPluginBySlug(event.pluginSlug));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectoryFetched(PluginStore.OnPluginDirectoryFetched event) {
        if (event.isError()) {
            return;
        }
        switch (event.type) {
            case NEW:
                mCanLoadMoreNewPlugins = event.canLoadMore;
                getIsLoadingMoreNewPlugins().setValue(false);
                reloadNewPlugins();
                break;
            case POPULAR:
                mCanLoadMorePopularPlugins = event.canLoadMore;
                getIsLoadingMorePopularPlugins().setValue(false);
                reloadPopularPlugins();
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectorySearched(PluginStore.OnPluginDirectorySearched event) {
        if (!event.isError()) {
            setSearchResults(event.searchTerm, event.plugins);
        }
    }

    // Search

    String getSearchQuery() {
        return mSearchQuery;
    }

    void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
    }

    void clearSearchResults() {
        getSearchResults().setValue(new ArrayList<WPOrgPluginModel>());
    }

    private void setSearchResults(String searchQuery, List<WPOrgPluginModel> searchResults) {
        if (mSearchQuery.equalsIgnoreCase(searchQuery)) {
            getSearchResults().setValue(searchResults);
        }
    }

    // Cache Management

    SitePluginModel getSitePluginFromSlug(String slug) {
        return mSitePluginsCache.get(slug);
    }

    // This method is specifically taking SitePluginModel as parameter, so it's understood that not all plugins
    // will be cached here
    @Nullable WPOrgPluginModel getCachedWPOrgPluginForSitePlugin(@NonNull SitePluginModel sitePlugin) {
        return mWPOrgPluginsForSitePluginsCache.get(sitePlugin.getSlug());
    }

    // In order to avoid hitting the DB in bindViewHolder multiple times for site plugins, we attempt to cache them here
    void cacheWPOrgPluginIfNecessary(WPOrgPluginModel wpOrgPlugin) {
        if (wpOrgPlugin == null) {
            return;
        }
        String slug = wpOrgPlugin.getSlug();
        if (mSitePluginsCache.containsKey(slug)) {
            mWPOrgPluginsForSitePluginsCache.put(slug, wpOrgPlugin);
        }
    }
}
