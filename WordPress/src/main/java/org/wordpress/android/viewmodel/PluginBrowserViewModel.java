package org.wordpress.android.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class PluginBrowserViewModel extends ViewModel {
    public enum PluginListType {
        SITE,
        FEATURED,
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
    // We don't want synthetic accessor methods to be introduced, so `protected` is used over `private`
    // and the warning suppressed
    @SuppressWarnings("WeakerAccess")
    protected final Set<String> mUpdatedPluginSlugSet;

    private final MutableLiveData<PluginListStatus> mFeaturedPluginsListStatus;
    private final MutableLiveData<PluginListStatus> mNewPluginsListStatus;
    private final MutableLiveData<PluginListStatus> mPopularPluginsListStatus;
    private final MutableLiveData<PluginListStatus> mSitePluginsListStatus;
    private final MutableLiveData<PluginListStatus> mSearchPluginsListStatus;

    private final MutableLiveData<List<ImmutablePluginModel>> mFeaturedPlugins;
    private final MutableLiveData<List<ImmutablePluginModel>> mNewPlugins;
    private final MutableLiveData<List<ImmutablePluginModel>> mPopularPlugins;
    private final MutableLiveData<List<ImmutablePluginModel>> mSitePlugins;
    private final MutableLiveData<List<ImmutablePluginModel>> mSearchResults;

    private final MutableLiveData<String> mTitle;

    @SuppressWarnings("WeakerAccess")
    @Inject
    public PluginBrowserViewModel(@NonNull Dispatcher dispatcher, @NonNull PluginStore pluginStore) {
        super();
        mDispatcher = dispatcher;
        mPluginStore = pluginStore;

        mDispatcher.register(this);

        mHandler = new Handler();
        mUpdatedPluginSlugSet = new HashSet<>();

        mFeaturedPlugins = new MutableLiveData<>();
        mSitePlugins = new MutableLiveData<>();
        mNewPlugins = new MutableLiveData<>();
        mPopularPlugins = new MutableLiveData<>();
        mSearchResults = new MutableLiveData<>();

        mFeaturedPluginsListStatus = new MutableLiveData<>();
        mNewPluginsListStatus = new MutableLiveData<>();
        mPopularPluginsListStatus = new MutableLiveData<>();
        mSitePluginsListStatus = new MutableLiveData<>();
        mSearchPluginsListStatus = new MutableLiveData<>();
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

    @WorkerThread
    public void start() {
        if (mIsStarted) {
            return;
        }
        reloadPluginDirectory(PluginDirectoryType.FEATURED);
        reloadPluginDirectory(PluginDirectoryType.NEW);
        reloadPluginDirectory(PluginDirectoryType.POPULAR);
        reloadPluginDirectory(PluginDirectoryType.SITE);

        fetchPlugins(PluginListType.SITE, false);
        fetchPlugins(PluginListType.FEATURED, false);
        fetchPlugins(PluginListType.POPULAR, false);
        fetchPlugins(PluginListType.NEW, false);
        // If activity is recreated we need to re-search
        if (shouldSearch()) {
            fetchPlugins(PluginListType.SEARCH, false);
        }

        mIsStarted = true;
    }

    // Site & WPOrg plugin management

    @WorkerThread
    private void reloadPluginDirectory(PluginDirectoryType directoryType) {
        List<ImmutablePluginModel> pluginList = mPluginStore.getPluginDirectory(getSite(), directoryType);
        switch (directoryType) {
            case FEATURED:
                mFeaturedPlugins.postValue(pluginList);
                break;
            case NEW:
                mNewPlugins.postValue(pluginList);
                break;
            case POPULAR:
                mPopularPlugins.postValue(pluginList);
                break;
            case SITE:
                mSitePlugins.postValue(pluginList);
                break;
        }
    }

    // Pull to refresh

    public void pullToRefresh(@NonNull PluginListType pluginListType) {
        fetchPlugins(pluginListType, false);
    }

    // Network Requests

    @WorkerThread
    private void fetchPlugins(@NonNull PluginListType listType, boolean loadMore) {
        if (!shouldFetchPlugins(listType, loadMore)) {
            return;
        }
        PluginListStatus newStatus = loadMore ? PluginListStatus.LOADING_MORE : PluginListStatus.FETCHING;
        switch (listType) {
            case SITE:
                mSitePluginsListStatus.postValue(newStatus);
                PluginStore.FetchPluginDirectoryPayload payload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.SITE, getSite(), loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload));
                break;
            case FEATURED:
                mFeaturedPluginsListStatus.postValue(newStatus);
                PluginStore.FetchPluginDirectoryPayload featuredPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, getSite(), loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload));
                break;
            case POPULAR:
                mPopularPluginsListStatus.postValue(newStatus);
                PluginStore.FetchPluginDirectoryPayload popularPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, getSite(), loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload));
                break;
            case NEW:
                mNewPluginsListStatus.postValue(newStatus);
                PluginStore.FetchPluginDirectoryPayload newPayload =
                        new PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.NEW, getSite(), loadMore);
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload));
                break;
            case SEARCH:
                mSearchPluginsListStatus.postValue(newStatus);
                PluginStore.SearchPluginDirectoryPayload searchPayload =
                        new PluginStore.SearchPluginDirectoryPayload(getSite(), getSearchQuery(), 1);
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload));
                break;
        }
    }

    @WorkerThread
    private boolean shouldFetchPlugins(PluginListType listType, boolean loadMore) {
        PluginListStatus currentStatus = null;
        switch (listType) {
            case SITE:
                currentStatus = getSitePluginsListStatus().getValue();
                break;
            case FEATURED:
                currentStatus = getFeaturedPluginsListStatus().getValue();
                break;
            case POPULAR:
                currentStatus = getPopularPluginsListStatus().getValue();
                break;
            case NEW:
                currentStatus = getNewPluginsListStatus().getValue();
                break;
            case SEARCH:
                // We should always do the initial search because the string might have changed and it is
                // already optimized in submitSearch with a delay. Even though FluxC allows it, we don't do multiple
                // pages of search, so if we are trying to load more, we can ignore it
                return !loadMore;
        }
        if (currentStatus == PluginListStatus.FETCHING || currentStatus == PluginListStatus.LOADING_MORE) {
            // if we are already fetching something we shouldn't start a new one. Even if we are loading more plugins
            // and the user pulled to refresh, we don't want (or need) the 2 requests colliding
            return false;
        }
        if (loadMore && currentStatus != PluginListStatus.CAN_LOAD_MORE) {
            // There is nothing to load more
            return false;
        }
        return true;
    }

    public void loadMore(PluginListType listType) {
        fetchPlugins(listType, true);
    }

    // Network Callbacks

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onWPOrgPluginFetched(PluginStore.OnWPOrgPluginFetched event) {
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS,
                    "An error occurred while fetching the wporg plugin with type: " + event.error.type);
            return;
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.pluginSlug) && mUpdatedPluginSlugSet.add(event.pluginSlug)) {
            updateAllPluginListsIfNecessary();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPluginDirectoryFetched(PluginStore.OnPluginDirectoryFetched event) {
        PluginListStatus listStatus;
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                                       + event.error.type);
            listStatus = PluginListStatus.ERROR;
        } else {
            listStatus = event.canLoadMore ? PluginListStatus.CAN_LOAD_MORE : PluginListStatus.DONE;
        }
        switch (event.type) {
            case FEATURED:
                mFeaturedPluginsListStatus.postValue(listStatus);
                break;
            case NEW:
                mNewPluginsListStatus.postValue(listStatus);
                break;
            case POPULAR:
                mPopularPluginsListStatus.postValue(listStatus);
                break;
            case SITE:
                mSitePluginsListStatus.postValue(listStatus);
        }
        if (!event.isError()) {
            reloadPluginDirectory(event.type);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPluginDirectorySearched(PluginStore.OnPluginDirectorySearched event) {
        if (mSearchQuery == null || !mSearchQuery.equals(event.searchTerm)) {
            return;
        }
        if (event.isError()) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while searching the plugin directory");
            mSearchPluginsListStatus.postValue(PluginListStatus.ERROR);
            return;
        }
        mSearchResults.postValue(event.plugins);
        mSearchPluginsListStatus.postValue(PluginListStatus.DONE);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSitePluginConfigured(PluginStore.OnSitePluginConfigured event) {
        if (event.isError()) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return;
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSitePluginDeleted(PluginStore.OnSitePluginDeleted event) {
        if (event.isError()) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return;
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSitePluginInstalled(PluginStore.OnSitePluginInstalled event) {
        if (event.isError()) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return;
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSitePluginUpdated(PluginStore.OnSitePluginUpdated event) {
        if (event.isError()) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return;
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary();
        }
    }

    // Keeping the data up to date

    @WorkerThread
    private void updateAllPluginListsIfNecessary() {
        final Set<String> copiedSet = new HashSet<>(mUpdatedPluginSlugSet);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Using the size of the set for comparison might fail since we clear the mUpdatedPluginSlugSet
                if (copiedSet.equals(mUpdatedPluginSlugSet)) {
                    updateAllPluginListsWithNewPlugins(copiedSet);
                    mUpdatedPluginSlugSet.clear();
                }
            }
        }, 250);
    }

    // We don't want synthetic accessor methods to be introduced, so `protected` is used over `private` and the
    // warning suppressed
    @WorkerThread
    @SuppressWarnings("WeakerAccess")
    protected void updateAllPluginListsWithNewPlugins(@NonNull Set<String> updatedPluginSlugSet) {
        if (updatedPluginSlugSet.size() == 0) {
            return;
        }
        Map<String, ImmutablePluginModel> newPluginMap = new HashMap<>(updatedPluginSlugSet.size());
        for (String slug : updatedPluginSlugSet) {
            ImmutablePluginModel immutablePlugin = mPluginStore.getImmutablePluginBySlug(getSite(), slug);
            if (immutablePlugin != null) {
                newPluginMap.put(slug, immutablePlugin);
            }
        }
        // By combining all the updated plugins into one map, we can post a single update to the UI after changes are
        // reflected
        updatePluginListWithNewPlugin(mFeaturedPlugins, newPluginMap);
        updatePluginListWithNewPlugin(mNewPlugins, newPluginMap);
        updatePluginListWithNewPlugin(mPopularPlugins, newPluginMap);
        updatePluginListWithNewPlugin(mSearchResults, newPluginMap);

        // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins can
        // mess up the list. Also we care most about the Site Plugins and using the store to get the correct plugin
        // information
        // is much more reliable than any manual update we can make
        reloadPluginDirectory(PluginDirectoryType.SITE);
    }

    @WorkerThread
    private void updatePluginListWithNewPlugin(
            @NonNull final MutableLiveData<List<ImmutablePluginModel>> mutableLiveData,
            @NonNull final Map<String, ImmutablePluginModel> newPluginMap) {
        List<ImmutablePluginModel> pluginList = mutableLiveData.getValue();
        if (pluginList == null || pluginList.size() == 0 || newPluginMap.size() == 0) {
            // Nothing to update
            return;
        }
        // When a site or wporg plugin is updated we need to update every occurrence of that item
        List<ImmutablePluginModel> newList = new ArrayList<>(pluginList.size());
        boolean isChanged = false;
        for (ImmutablePluginModel immutablePlugin : pluginList) {
            String slug = immutablePlugin.getSlug();
            ImmutablePluginModel newPlugin = newPluginMap.get(slug);
            if (newPlugin != null) {
                // add new item
                newList.add(newPlugin);
                isChanged = true;
            } else {
                // add old item
                newList.add(immutablePlugin);
            }
        }
        // Only update if the list is actually changed
        if (isChanged) {
            mutableLiveData.postValue(newList);
        }
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

    // We don't want synthetic accessor methods to be introduced, so `protected` is used over `private` and the
    // warning suppressed
    @SuppressWarnings("WeakerAccess")
    @WorkerThread
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
                mSearchPluginsListStatus.postValue(PluginListStatus.DONE);
            }
        }
    }

    @WorkerThread
    private void clearSearchResults() {
        mSearchResults.postValue(new ArrayList<ImmutablePluginModel>());
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

    public LiveData<List<ImmutablePluginModel>> getSitePlugins() {
        return mSitePlugins;
    }

    public boolean isSitePluginsEmpty() {
        return getSitePlugins().getValue() == null || getSitePlugins().getValue().size() == 0;
    }

    public LiveData<List<ImmutablePluginModel>> getFeaturedPlugins() {
        return mFeaturedPlugins;
    }

    public LiveData<List<ImmutablePluginModel>> getNewPlugins() {
        return mNewPlugins;
    }

    public LiveData<List<ImmutablePluginModel>> getPopularPlugins() {
        return mPopularPlugins;
    }

    public LiveData<List<ImmutablePluginModel>> getSearchResults() {
        return mSearchResults;
    }

    public LiveData<PluginListStatus> getFeaturedPluginsListStatus() {
        return mFeaturedPluginsListStatus;
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

    public void setTitle(String title) {
        mTitle.postValue(title);
    }

    public LiveData<String> getTitle() {
        return mTitle;
    }

    public List<ImmutablePluginModel> getPluginsForListType(PluginListType listType) {
        switch (listType) {
            case SITE:
                return getSitePlugins().getValue();
            case FEATURED:
                return getFeaturedPlugins().getValue();
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
