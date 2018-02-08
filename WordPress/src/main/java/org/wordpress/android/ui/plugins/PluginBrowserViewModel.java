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
import java.util.Map;

import javax.inject.Inject;

public class PluginBrowserViewModel extends AndroidViewModel {
    @Inject Dispatcher mDispatcher;
    @Inject PluginStore mPluginStore;

    private String mSearchQuery;
    private SiteModel mSite;

    private boolean mCanLoadMoreNewPlugins = true;
    private boolean mCanLoadMorePopularPlugins = true;

    private Map<PluginBrowserActivity.PluginListType, Boolean> mLoadingMorePlugins = new HashMap<>();

    private final HashMap<String, SitePluginModel> mSitePluginsMap = new HashMap<>();
    private final HashMap<String, WPOrgPluginModel> mWPOrgPluginsForSitePluginsMap = new HashMap<>();
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

    void loadMore(PluginBrowserActivity.PluginListType listType) {
        fetchPlugins(listType, true);
    }

    void reloadAndFetchAllPlugins() {
        reloadAllPluginsFromStore();

        fetchPlugins(PluginBrowserActivity.PluginListType.SITE, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.POPULAR, false);
        fetchPlugins(PluginBrowserActivity.PluginListType.NEW, false);
    }

    String getSearchQuery() {
        return mSearchQuery;
    }

    void setSearchQuery(String searchQuery) {
        mSearchQuery = searchQuery;
    }

    SiteModel getSite() {
        return mSite;
    }

    void setSite(SiteModel site) {
        mSite = site;
    }

    // Site & WPOrg plugin management

    SitePluginModel getSitePluginFromSlug(String slug) {
        return mSitePluginsMap.get(slug);
    }

    // This method is specifically taking SitePluginModel as parameter, so it's understood that not all plugins
    // will be cached here
    @Nullable WPOrgPluginModel getCachedWPOrgPluginForSitePlugin(@NonNull SitePluginModel sitePlugin) {
        return mWPOrgPluginsForSitePluginsMap.get(sitePlugin.getSlug());
    }

    // In order to avoid hitting the DB in bindViewHolder multiple times for site plugins, we attempt to cache them here
    void cacheWPOrgPluginIfNecessary(WPOrgPluginModel wpOrgPlugin) {
        if (wpOrgPlugin == null) {
            return;
        }
        String slug = wpOrgPlugin.getSlug();
        if (mSitePluginsMap.containsKey(slug)) {
            mWPOrgPluginsForSitePluginsMap.put(slug, wpOrgPlugin);
        }
    }

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
        mSitePluginsMap.clear();
        for (SitePluginModel plugin: sitePlugins) {
            mSitePluginsMap.put(plugin.getSlug(), plugin);
        }
        getSitePlugins().setValue(sitePlugins);
    }

    private void reloadNewPlugins() {
        getNewPlugins().setValue(mPluginStore.getPluginDirectory(PluginDirectoryType.NEW));
    }

    private void reloadPopularPlugins() {
        getPopularPlugins().setValue(mPluginStore.getPluginDirectory(PluginDirectoryType.POPULAR));
    }

    void clearSearchResults() {
        getSearchResults().setValue(new ArrayList<WPOrgPluginModel>());
    }

    private void setSearchResults(String searchQuery, List<WPOrgPluginModel> searchResults) {
        if (mSearchQuery.equalsIgnoreCase(searchQuery)) {
            getSearchResults().setValue(searchResults);
        }
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

    private boolean isLoadingMorePlugins(PluginBrowserActivity.PluginListType listType) {
        return mLoadingMorePlugins.get(listType);
    }

    // Network

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
                mLoadingMorePlugins.put(PluginBrowserActivity.PluginListType.POPULAR, loadMore);
                PluginStore.FetchPluginDirectoryPayload popularPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload));
                break;
            case NEW:
                mLoadingMorePlugins.put(PluginBrowserActivity.PluginListType.NEW, loadMore);
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSitePluginsFetched(PluginStore.OnSitePluginsFetched event) {
        if (event.isError()) {
            // todo: handle the error
            return;
        }
        reloadSitePlugins();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectoryFetched(PluginStore.OnPluginDirectoryFetched event) {
        if (event.isError()) {
            // todo: handle error
            return;
        }
        switch (event.type) {
            case NEW:
                mCanLoadMoreNewPlugins = event.canLoadMore;
                mLoadingMorePlugins.put(PluginBrowserActivity.PluginListType.NEW, false);
                reloadNewPlugins();
                break;
            case POPULAR:
                mCanLoadMorePopularPlugins = event.canLoadMore;
                mLoadingMorePlugins.put(PluginBrowserActivity.PluginListType.POPULAR, false);
                reloadPopularPlugins();
                break;
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPluginDirectorySearched(PluginStore.OnPluginDirectorySearched event) {
        if (event.isError()) {
            // todo: handle error
            return;
        }
        setSearchResults(event.searchTerm, event.plugins);
    }
}
