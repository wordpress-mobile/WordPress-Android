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
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginDirectoryPayload
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectoryFetched
import org.wordpress.android.fluxc.store.PluginStore.OnPluginDirectorySearched
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginConfigured
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginDeleted
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginInstalled
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated
import org.wordpress.android.fluxc.store.PluginStore.OnWPOrgPluginFetched
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.inject.Inject
import kotlin.properties.Delegates

private const val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"
private const val KEY_TITLE = "KEY_TITLE"

@WorkerThread
class PluginBrowserViewModel @Inject constructor(
    private val mDispatcher: Dispatcher,
    private val mPluginStore: PluginStore,
    private val mSiteStore: SiteStore
) : ViewModel() {
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

    private var isStarted = false

    private val handler = Handler()
    private val updatedPluginSlugSet = HashSet<String>()

    private val _featuredPlugins = MutableLiveData<List<ImmutablePluginModel>>()
    val featuredPlugins: LiveData<List<ImmutablePluginModel>>
        get() = _featuredPlugins

    private val _popularPlugins = MutableLiveData<List<ImmutablePluginModel>>()
    val popularPlugins: LiveData<List<ImmutablePluginModel>>
        get() = _popularPlugins

    private val _newPlugins = MutableLiveData<List<ImmutablePluginModel>>()
    val newPlugins: LiveData<List<ImmutablePluginModel>>
        get() = _newPlugins

    private val _sitePlugins = MutableLiveData<List<ImmutablePluginModel>>()
    val sitePlugins: LiveData<List<ImmutablePluginModel>>
        get() = _sitePlugins
    val isSitePluginsEmpty: Boolean
        get() = sitePlugins.value == null || sitePlugins.value!!.isEmpty()

    private val _searchResults = MutableLiveData<List<ImmutablePluginModel>>()
    val searchResults: LiveData<List<ImmutablePluginModel>>
        get() = _searchResults

    private val _featuredPluginsListStatus = MutableLiveData<PluginListStatus>()
    val featuredPluginsListStatus: LiveData<PluginListStatus>
        get() = _featuredPluginsListStatus

    private val _newPluginsListStatus = MutableLiveData<PluginListStatus>()
    val newPluginsListStatus: LiveData<PluginListStatus>
        get() = _newPluginsListStatus

    private val _popularPluginsListStatus = MutableLiveData<PluginListStatus>()
    val popularPluginsListStatus: LiveData<PluginListStatus>
        get() = _popularPluginsListStatus

    private val _sitePluginsListStatus = MutableLiveData<PluginListStatus>()
    val sitePluginsListStatus: LiveData<PluginListStatus>
        get() = _sitePluginsListStatus

    private val _searchPluginsListStatus = MutableLiveData<PluginListStatus>()
    val searchPluginsListStatus: LiveData<PluginListStatus>
        get() = _searchPluginsListStatus

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    var site: SiteModel? = null

    var searchQuery: String by Delegates.observable("") {
        _, oldValue, newValue ->
        if (newValue != oldValue) {
            submitSearch(newValue, true)
        }
    }

    private val shouldSearch: Boolean
        get() = searchQuery.length > 1 // We need at least 2 characters to be able to search plugins

    init {
        mDispatcher.register(this)
    }

    override fun onCleared() {
        mDispatcher.unregister(this)
        super.onCleared()
    }

    fun writeToBundle(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)
        outState.putString(KEY_SEARCH_QUERY, searchQuery)
        outState.putString(KEY_TITLE, _title.value)
    }

    fun readFromBundle(savedInstanceState: Bundle) {
        if (isStarted) {
            // This was called due to a config change where the data survived, we don't need to
            // read from the bundle
            return
        }
        site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
        searchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY)
        setTitle(savedInstanceState.getString(KEY_TITLE))
    }

    fun start() {
        if (isStarted) {
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

        isStarted = true
    }

    // Site & WPOrg plugin management

    private fun reloadPluginDirectory(directoryType: PluginDirectoryType) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(site!!, directoryType)
            when (directoryType) {
                PluginDirectoryType.FEATURED -> _featuredPlugins.postValue(pluginList)
                PluginDirectoryType.NEW -> _newPlugins.postValue(pluginList)
                PluginDirectoryType.POPULAR -> _popularPlugins.postValue(pluginList)
                PluginDirectoryType.SITE -> _sitePlugins.postValue(pluginList)
            }
        }
    }

    // Pull to refresh

    fun pullToRefresh(pluginListType: PluginListType) {
        fetchPlugins(pluginListType, false)
    }

    // Network Requests

    private fun fetchPlugins(listType: PluginListType, loadMore: Boolean) {
        if (!shouldFetchPlugins(listType, loadMore)) {
            return
        }
        val newStatus = if (loadMore) PluginListStatus.LOADING_MORE else PluginListStatus.FETCHING
        when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> {
                _sitePluginsListStatus.postValue(newStatus)
                val payload = FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            PluginBrowserViewModel.PluginListType.FEATURED -> {
                _featuredPluginsListStatus.postValue(newStatus)
                val featuredPayload = FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            PluginBrowserViewModel.PluginListType.POPULAR -> {
                _popularPluginsListStatus.postValue(newStatus)
                val popularPayload = FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            PluginBrowserViewModel.PluginListType.NEW -> {
                _newPluginsListStatus.postValue(newStatus)
                val newPayload = FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            PluginBrowserViewModel.PluginListType.SEARCH -> {
                _searchPluginsListStatus.postValue(newStatus)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        if (listType == PluginListType.SITE && SiteUtils.isNonAtomicBusinessPlanSite(site)) {
            return false
        }
        val currentStatus = when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> sitePluginsListStatus.value
            PluginBrowserViewModel.PluginListType.FEATURED -> featuredPluginsListStatus.value
            PluginBrowserViewModel.PluginListType.POPULAR -> popularPluginsListStatus.value
            PluginBrowserViewModel.PluginListType.NEW -> newPluginsListStatus.value
            // We should always do the initial search because the string might have changed and it is
            // already optimized in submitSearch with a delay. Even though FluxC allows it, we don't do multiple
            // pages of search, so if we are trying to load more, we can ignore it
            PluginBrowserViewModel.PluginListType.SEARCH -> return !loadMore
        }
        if (currentStatus == PluginListStatus.FETCHING || currentStatus == PluginListStatus.LOADING_MORE) {
            // if we are already fetching something we shouldn't start a new one. Even if we are loading more plugins
            // and the user pulled to refresh, we don't want (or need) the 2 requests colliding
            return false
        }
        return !(loadMore && currentStatus != PluginListStatus.CAN_LOAD_MORE)
    }

    fun loadMore(listType: PluginListType) {
        fetchPlugins(listType, true)
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onWPOrgPluginFetched(event: OnWPOrgPluginFetched) {
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the wporg plugin with type: " + event.error.type)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!event.pluginSlug.isNullOrEmpty() && updatedPluginSlugSet.add(event.pluginSlug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onPluginDirectoryFetched(event: OnPluginDirectoryFetched) {
        val listStatus = if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": " +
                    event.error.type)
            PluginListStatus.ERROR
        } else {
            if (event.canLoadMore) PluginListStatus.CAN_LOAD_MORE else PluginListStatus.DONE
        }
        when (event.type) {
            PluginDirectoryType.FEATURED -> _featuredPluginsListStatus.postValue(listStatus)
            PluginDirectoryType.NEW -> _newPluginsListStatus.postValue(listStatus)
            PluginDirectoryType.POPULAR -> _popularPluginsListStatus.postValue(listStatus)
            PluginDirectoryType.SITE -> _sitePluginsListStatus.postValue(listStatus)
            null -> AppLog.d(T.PLUGINS, "Plugin directory type shouldn't be null")
        }
        if (!event.isError) {
            reloadPluginDirectory(event.type)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onPluginDirectorySearched(event: OnPluginDirectorySearched) {
        if (searchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while searching the plugin directory")
            _searchPluginsListStatus.postValue(PluginListStatus.ERROR)
            return
        }
        _searchResults.postValue(event.plugins)
        _searchPluginsListStatus.postValue(PluginListStatus.DONE)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onSitePluginConfigured(event: OnSitePluginConfigured) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onSitePluginDeleted(event: OnSitePluginDeleted) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onSitePluginInstalled(event: OnSitePluginInstalled) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onSitePluginUpdated(event: OnSitePluginUpdated) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        // Check if the slug is empty, if not add it to the set and only trigger the update
        // if the slug is not in the set
        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
            updateAllPluginListsIfNecessary()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            // The error should be safe to ignore since we are not triggering the action and there is nothing we need
            // to do about it
            return
        }

        val siteId = site?.siteId
        siteId?.let {
            site = mSiteStore.getSiteBySiteId(siteId)
        }
    }

    // Keeping the data up to date

    private fun updateAllPluginListsIfNecessary() {
        val copiedSet = HashSet(updatedPluginSlugSet)
        handler.postDelayed({
            // Using the size of the set for comparison might fail since we clear the updatedPluginSlugSet
            if (copiedSet == updatedPluginSlugSet) {
                updateAllPluginListsWithNewPlugins(copiedSet)
                updatedPluginSlugSet.clear()
            }
        }, 250)
    }

    private fun updateAllPluginListsWithNewPlugins(updatedPluginSlugSet: Set<String>) {
        if (updatedPluginSlugSet.isEmpty()) {
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
        updatePluginListWithNewPlugin(_featuredPlugins, newPluginMap)
        updatePluginListWithNewPlugin(_newPlugins, newPluginMap)
        updatePluginListWithNewPlugin(_popularPlugins, newPluginMap)
        updatePluginListWithNewPlugin(_searchResults, newPluginMap)

        // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins can
        // mess up the list. Also we care most about the Site Plugins and using the store to get the correct plugin
        // information is much more reliable than any manual update we can make
        reloadPluginDirectory(PluginDirectoryType.SITE)
    }

    private fun updatePluginListWithNewPlugin(
        mutableLiveData: MutableLiveData<List<ImmutablePluginModel>>,
        newPluginMap: Map<String, ImmutablePluginModel>
    ) {
        val pluginList = mutableLiveData.value
        if (pluginList == null || pluginList.isEmpty() || newPluginMap.isEmpty()) {
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

    private fun submitSearch(query: String, delayed: Boolean) {
        // If the query is not long enough we don't need to delay it
        if (delayed && shouldSearch) {
            handler.postDelayed({
                if (query == searchQuery) {
                    submitSearch(query, false)
                }
            }, 250)
        } else {
            clearSearchResults()

            if (shouldSearch) {
                fetchPlugins(PluginListType.SEARCH, false)
            } else {
                // Due to the query being changed after the last fetch, the status won't ever be updated, so we need
                // to manually do it. Consider the following case:
                // 1. Search the plugins for "contact" which will change the status to FETCHING
                // 2. Before the fetch completes delete the text
                // 3. In `onPluginDirectorySearched` the result will be ignored, because the query changed, but it won't
                // be triggered again, because another fetch didn't happen (due to query being empty)
                // 4. The status will be stuck in FETCHING until another search occurs. The following reset fixes the
                // problem.
                _searchPluginsListStatus.postValue(PluginListStatus.DONE)
            }
        }
    }

    private fun clearSearchResults() {
        _searchResults.postValue(ArrayList())
    }

    fun shouldShowEmptySearchResultsView(): Boolean {
        // Search query is less than 2 characters
        if (!shouldSearch) {
            return false
        }
        return if (searchPluginsListStatus.value != PluginListStatus.DONE &&
                searchPluginsListStatus.value != PluginListStatus.ERROR) {
            false
        } else searchResults.value == null || searchResults.value!!.isEmpty()
    }

    fun setTitle(title: String?) {
        _title.postValue(title)
    }

    fun getPluginsForListType(listType: PluginListType): List<ImmutablePluginModel>? {
        return when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> sitePlugins.value
            PluginBrowserViewModel.PluginListType.FEATURED -> featuredPlugins.value
            PluginBrowserViewModel.PluginListType.POPULAR -> popularPlugins.value
            PluginBrowserViewModel.PluginListType.NEW -> newPlugins.value
            PluginBrowserViewModel.PluginListType.SEARCH -> searchResults.value
        }
    }
}
