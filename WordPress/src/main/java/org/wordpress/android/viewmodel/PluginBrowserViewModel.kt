package org.wordpress.android.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.os.Handler
import android.support.annotation.WorkerThread
import android.text.TextUtils

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.StringUtils

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

class PluginBrowserViewModel @Inject
constructor(private val mDispatcher: Dispatcher, private val mPluginStore: PluginStore) : ViewModel() {
    private var mIsStarted = false
    private var mSearchQuery: String? = null
    // Simple Getters & Setters

    var site: SiteModel? = null

    private val mHandler: Handler
    // We don't want synthetic accessor methods to be introduced, so `protected` is used over `private`
    // and the warning suppressed
    protected val mUpdatedPluginSlugSet: MutableSet<String>

    private val mFeaturedPluginsListStatus: MutableLiveData<PluginListStatus>
    private val mNewPluginsListStatus: MutableLiveData<PluginListStatus>
    private val mPopularPluginsListStatus: MutableLiveData<PluginListStatus>
    private val mSitePluginsListStatus: MutableLiveData<PluginListStatus>
    private val mSearchPluginsListStatus: MutableLiveData<PluginListStatus>

    private val mFeaturedPlugins: MutableLiveData<List<ImmutablePluginModel>>
    private val mNewPlugins: MutableLiveData<List<ImmutablePluginModel>>
    private val mPopularPlugins: MutableLiveData<List<ImmutablePluginModel>>
    private val mSitePlugins: MutableLiveData<List<ImmutablePluginModel>>
    private val mSearchResults: MutableLiveData<List<ImmutablePluginModel>>

    private val mTitle: MutableLiveData<String>

    // Search

    var searchQuery: String?
        get() = mSearchQuery
        set(searchQuery) {
            mSearchQuery = searchQuery
            // Don't delay if the searchQuery is empty
            submitSearch(searchQuery, !TextUtils.isEmpty(searchQuery))
        }

    val sitePlugins: LiveData<List<ImmutablePluginModel>>
        get() = mSitePlugins

    val isSitePluginsEmpty: Boolean
        get() = sitePlugins.value == null || sitePlugins.value!!.size == 0

    val featuredPlugins: LiveData<List<ImmutablePluginModel>>
        get() = mFeaturedPlugins

    val newPlugins: LiveData<List<ImmutablePluginModel>>
        get() = mNewPlugins

    val popularPlugins: LiveData<List<ImmutablePluginModel>>
        get() = mPopularPlugins

    val searchResults: LiveData<List<ImmutablePluginModel>>
        get() = mSearchResults

    val featuredPluginsListStatus: LiveData<PluginListStatus>
        get() = mFeaturedPluginsListStatus

    val newPluginsListStatus: LiveData<PluginListStatus>
        get() = mNewPluginsListStatus

    val popularPluginsListStatus: LiveData<PluginListStatus>
        get() = mPopularPluginsListStatus

    val sitePluginsListStatus: LiveData<PluginListStatus>
        get() = mSitePluginsListStatus

    val searchPluginsListStatus: LiveData<PluginListStatus>
        get() = mSearchPluginsListStatus

    val title: LiveData<String>
        get() = mTitle

    enum class PluginListType {
        SITE,
        FEATURED,
        POPULAR,
        NEW,
        SEARCH
    }

    enum class PluginListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    init {
        mDispatcher.register(this)

        mHandler = Handler()
        mUpdatedPluginSlugSet = HashSet()

        mFeaturedPlugins = MutableLiveData()
        mSitePlugins = MutableLiveData()
        mNewPlugins = MutableLiveData()
        mPopularPlugins = MutableLiveData()
        mSearchResults = MutableLiveData()

        mFeaturedPluginsListStatus = MutableLiveData()
        mNewPluginsListStatus = MutableLiveData()
        mPopularPluginsListStatus = MutableLiveData()
        mSitePluginsListStatus = MutableLiveData()
        mSearchPluginsListStatus = MutableLiveData()
        mTitle = MutableLiveData()
    }

    override fun onCleared() {
        super.onCleared()
        mDispatcher.unregister(this)
    }

    fun writeToBundle(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery)
        outState.putString(KEY_TITLE, mTitle.value)
    }

    fun readFromBundle(savedInstanceState: Bundle) {
        if (mIsStarted) {
            // This was called due to a config change where the data survived, we don't need to
            // read from the bundle
            return
        }
        site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY)
        setTitle(savedInstanceState.getString(KEY_TITLE))
    }

    @WorkerThread
    fun start() {
        if (mIsStarted) {
            return
        }
        reloadPluginDirectory(PluginDirectoryType.FEATURED)
        reloadPluginDirectory(PluginDirectoryType.NEW)
        reloadPluginDirectory(PluginDirectoryType.POPULAR)
        reloadPluginDirectory(PluginDirectoryType.SITE)

        fetchPlugins(PluginListType.SITE, false)
        fetchPlugins(PluginListType.FEATURED, false)
        fetchPlugins(PluginListType.POPULAR, false)
        fetchPlugins(PluginListType.NEW, false)
        // If activity is recreated we need to re-search
        if (shouldSearch()) {
            fetchPlugins(PluginListType.SEARCH, false)
        }

        mIsStarted = true
    }

    // Site & WPOrg plugin management

    @WorkerThread
    private fun reloadPluginDirectory(directoryType: PluginDirectoryType) {
        val pluginList = mPluginStore.getPluginDirectory(site!!, directoryType)
        when (directoryType) {
            PluginDirectoryType.FEATURED -> mFeaturedPlugins.postValue(pluginList)
            PluginDirectoryType.NEW -> mNewPlugins.postValue(pluginList)
            PluginDirectoryType.POPULAR -> mPopularPlugins.postValue(pluginList)
            PluginDirectoryType.SITE -> mSitePlugins.postValue(pluginList)
        }
    }

    // Pull to refresh

    fun pullToRefresh(pluginListType: PluginListType) {
        fetchPlugins(pluginListType, false)
    }

    // Network Requests

    @WorkerThread
    private fun fetchPlugins(listType: PluginListType, loadMore: Boolean) {
        if (!shouldFetchPlugins(listType, loadMore)) {
            return
        }
        val newStatus = if (loadMore) PluginListStatus.LOADING_MORE else PluginListStatus.FETCHING
        when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> {
                mSitePluginsListStatus.postValue(newStatus)
                val payload = PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            PluginBrowserViewModel.PluginListType.FEATURED -> {
                mFeaturedPluginsListStatus.postValue(newStatus)
                val featuredPayload = PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            PluginBrowserViewModel.PluginListType.POPULAR -> {
                mPopularPluginsListStatus.postValue(newStatus)
                val popularPayload = PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            PluginBrowserViewModel.PluginListType.NEW -> {
                mNewPluginsListStatus.postValue(newStatus)
                val newPayload = PluginStore.FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            PluginBrowserViewModel.PluginListType.SEARCH -> {
                mSearchPluginsListStatus.postValue(newStatus)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    @WorkerThread
    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        var currentStatus: PluginListStatus? = null
        when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> currentStatus = sitePluginsListStatus.value
            PluginBrowserViewModel.PluginListType.FEATURED -> currentStatus = featuredPluginsListStatus.value
            PluginBrowserViewModel.PluginListType.POPULAR -> currentStatus = popularPluginsListStatus.value
            PluginBrowserViewModel.PluginListType.NEW -> currentStatus = newPluginsListStatus.value
            PluginBrowserViewModel.PluginListType.SEARCH ->
                // We should always do the initial search because the string might have changed and it is
                // already optimized in submitSearch with a delay. Even though FluxC allows it, we don't do multiple
                // pages of search, so if we are trying to load more, we can ignore it
                return !loadMore
        }
        if (currentStatus == PluginListStatus.FETCHING || currentStatus == PluginListStatus.LOADING_MORE) {
            // if we are already fetching something we shouldn't start a new one. Even if we are loading more plugins
            // and the user pulled to refresh, we don't want (or need) the 2 requests colliding
            return false
        }
        return if (loadMore && currentStatus != PluginListStatus.CAN_LOAD_MORE) {
            // There is nothing to load more
            false
        } else true
    }

    fun loadMore(listType: PluginListType) {
        fetchPlugins(listType, true)
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onWPOrgPluginFetched(event: PluginStore.OnWPOrgPluginFetched) {
        if (event.isError) {
            AppLog.e(AppLog.T.PLUGINS,
                    "An error occurred while fetching the wporg plugin with type: " + event.error.type)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.pluginSlug) && mUpdatedPluginSlugSet.add(event.pluginSlug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPluginDirectoryFetched(event: PluginStore.OnPluginDirectoryFetched) {
        val listStatus: PluginListStatus
        if (event.isError) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                    + event.error.type)
            listStatus = PluginListStatus.ERROR
        } else {
            listStatus = if (event.canLoadMore) PluginListStatus.CAN_LOAD_MORE else PluginListStatus.DONE
        }
        when (event.type) {
            PluginDirectoryType.FEATURED -> mFeaturedPluginsListStatus.postValue(listStatus)
            PluginDirectoryType.NEW -> mNewPluginsListStatus.postValue(listStatus)
            PluginDirectoryType.POPULAR -> mPopularPluginsListStatus.postValue(listStatus)
            PluginDirectoryType.SITE -> mSitePluginsListStatus.postValue(listStatus)
        }
        if (!event.isError) {
            reloadPluginDirectory(event.type)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onPluginDirectorySearched(event: PluginStore.OnPluginDirectorySearched) {
        if (mSearchQuery == null || mSearchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(AppLog.T.PLUGINS, "An error occurred while searching the plugin directory")
            mSearchPluginsListStatus.postValue(PluginListStatus.ERROR)
            return
        }
        mSearchResults.postValue(event.plugins)
        mSearchPluginsListStatus.postValue(PluginListStatus.DONE)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSitePluginConfigured(event: PluginStore.OnSitePluginConfigured) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSitePluginDeleted(event: PluginStore.OnSitePluginDeleted) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSitePluginInstalled(event: PluginStore.OnSitePluginInstalled) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSitePluginUpdated(event: PluginStore.OnSitePluginUpdated) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && mUpdatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    // Keeping the data up to date

    @WorkerThread
    private fun updateAllPluginListsIfNecessary() {
        val copiedSet = HashSet(mUpdatedPluginSlugSet)
        mHandler.postDelayed({
            // Using the size of the set for comparison might fail since we clear the mUpdatedPluginSlugSet
            if (copiedSet == mUpdatedPluginSlugSet) {
                updateAllPluginListsWithNewPlugins(copiedSet)
                mUpdatedPluginSlugSet.clear()
            }
        }, 250)
    }

    // We don't want synthetic accessor methods to be introduced, so `protected` is used over `private` and the
    // warning suppressed
    @WorkerThread
    protected fun updateAllPluginListsWithNewPlugins(updatedPluginSlugSet: Set<String>) {
        if (updatedPluginSlugSet.size == 0) {
            return
        }
        val newPluginMap = HashMap<String, ImmutablePluginModel>(updatedPluginSlugSet.size)
        for (slug in updatedPluginSlugSet) {
            val immutablePlugin = mPluginStore.getImmutablePluginBySlug(site!!, slug)
            if (immutablePlugin != null) {
                newPluginMap[slug] = immutablePlugin
            }
        }
        // By combining all the updated plugins into one map, we can post a single update to the UI after changes are
        // reflected
        updatePluginListWithNewPlugin(mFeaturedPlugins, newPluginMap)
        updatePluginListWithNewPlugin(mNewPlugins, newPluginMap)
        updatePluginListWithNewPlugin(mPopularPlugins, newPluginMap)
        updatePluginListWithNewPlugin(mSearchResults, newPluginMap)

        // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins can
        // mess up the list. Also we care most about the Site Plugins and using the store to get the correct plugin
        // information
        // is much more reliable than any manual update we can make
        reloadPluginDirectory(PluginDirectoryType.SITE)
    }

    @WorkerThread
    private fun updatePluginListWithNewPlugin(
            mutableLiveData: MutableLiveData<List<ImmutablePluginModel>>,
            newPluginMap: Map<String, ImmutablePluginModel>) {
        val pluginList = mutableLiveData.value
        if (pluginList == null || pluginList.size == 0 || newPluginMap.size == 0) {
            // Nothing to update
            return
        }
        // When a site or wporg plugin is updated we need to update every occurrence of that item
        val newList = ArrayList<ImmutablePluginModel>(pluginList.size)
        var isChanged = false
        for (immutablePlugin in pluginList) {
            val slug = immutablePlugin.slug
            val newPlugin = newPluginMap[slug]
            if (newPlugin != null) {
                // add new item
                newList.add(newPlugin)
                isChanged = true
            } else {
                // add old item
                newList.add(immutablePlugin)
            }
        }
        // Only update if the list is actually changed
        if (isChanged) {
            mutableLiveData.postValue(newList)
        }
    }

    private fun shouldSearch(): Boolean {
        // We need at least 2 characters to be able to search plugins
        return searchQuery != null && searchQuery!!.length > 1
    }

    // We don't want synthetic accessor methods to be introduced, so `protected` is used over `private` and the
    // warning suppressed
    @WorkerThread
    protected fun submitSearch(query: String?, delayed: Boolean) {
        if (delayed) {
            mHandler.postDelayed({
                if (StringUtils.equals(query, searchQuery)) {
                    submitSearch(query, false)
                }
            }, 250)
        } else {
            clearSearchResults()

            if (shouldSearch()) {
                fetchPlugins(PluginListType.SEARCH, false)
            } else {
                // Due to the query being changed after the last fetch, the status won't ever be updated, so we need
                // to manually do it. Consider the following case:
                // 1. Search the plugins for "contact" which will change the status to FETCHING
                // 2. Before the fetch completes delete the text
                // 3. In `onPluginDirectorySearched` the result will be ignored, because the query changed, but it won't
                // be triggered again, because another fetch didn't happen (due to query being empty)
                // 4. The status will be stuck in FETCHING until another search occurs. This following reset fixes the
                // problem.
                mSearchPluginsListStatus.postValue(PluginListStatus.DONE)
            }
        }
    }

    @WorkerThread
    private fun clearSearchResults() {
        mSearchResults.postValue(ArrayList())
    }

    fun shouldShowEmptySearchResultsView(): Boolean {
        // Search query is less than 2 characters
        if (!shouldSearch()) {
            return false
        }
        return if (mSearchPluginsListStatus.value != PluginListStatus.DONE && mSearchPluginsListStatus.value != PluginListStatus.ERROR) {
            false
        } else searchResults.value == null || searchResults.value!!.size == 0
    }

    fun setTitle(title: String?) {
        mTitle.postValue(title)
    }

    fun getPluginsForListType(listType: PluginListType): List<ImmutablePluginModel>? {
        when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> return sitePlugins.value
            PluginBrowserViewModel.PluginListType.FEATURED -> return featuredPlugins.value
            PluginBrowserViewModel.PluginListType.POPULAR -> return popularPlugins.value
            PluginBrowserViewModel.PluginListType.NEW -> return newPlugins.value
            PluginBrowserViewModel.PluginListType.SEARCH -> return searchResults.value
        }
        return null
    }

    companion object {
        private val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"
        private val KEY_TITLE = "KEY_TITLE"
    }
}
