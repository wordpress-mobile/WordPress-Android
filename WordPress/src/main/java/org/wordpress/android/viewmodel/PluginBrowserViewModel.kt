package org.wordpress.android.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.os.Handler
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
import org.wordpress.android.models.networkresource.NetworkResource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.ArrayList
import java.util.HashSet
import javax.inject.Inject
import kotlin.properties.Delegates

private const val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"
private const val KEY_TITLE = "KEY_TITLE"

typealias PluginList = List<ImmutablePluginModel>
typealias PluginListNetworkResource = NetworkResource<PluginList>

class PluginListLiveData: MutableLiveData<PluginListNetworkResource>() {
    init {
        value = NetworkResource.Init()
    }

    override fun getValue(): PluginListNetworkResource {
        return super.getValue()!!
    }
}

class PluginBrowserViewModel @Inject
constructor(private val mDispatcher: Dispatcher, private val mPluginStore: PluginStore) : ViewModel() {
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

    val ffPlugins = PluginListLiveData()
    val ppPlugins = PluginListLiveData()
    val nnPlugins = PluginListLiveData()
    val ssPlugins = PluginListLiveData()

    val isSitePluginsEmpty: Boolean
        get() {
            ssPlugins.value.data?.let {
                return it.isEmpty()
            }
            return true
        }

    private val _searchResults = MutableLiveData<List<ImmutablePluginModel>>()
    val searchResults: LiveData<List<ImmutablePluginModel>>
        get() = _searchResults

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
        readyDirectory(PluginDirectoryType.FEATURED)
        readyDirectory(PluginDirectoryType.NEW)
        readyDirectory(PluginDirectoryType.POPULAR)
        readyDirectory(PluginDirectoryType.SITE)

        fetchPlugins(PluginListType.SITE, false)
        fetchPlugins(PluginListType.FEATURED, false)
        fetchPlugins(PluginListType.POPULAR, false)
        fetchPlugins(PluginListType.NEW, false)

        isStarted = true
    }

    // Site & WPOrg plugin management

    private fun readyDirectory(directoryType: PluginDirectoryType) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(it, directoryType)
            getPluginListLiveData(directoryType).value = NetworkResource.Ready(pluginList)
        }
    }

    private fun successDirectory(directoryType: PluginDirectoryType, canLoadMore: Boolean) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(it, directoryType)
            getPluginListLiveData(directoryType).value = NetworkResource.Success(pluginList, canLoadMore)
        }
    }

    private fun getPluginListLiveData(directoryType: PluginDirectoryType): PluginListLiveData {
        return when (directoryType) {
            PluginDirectoryType.FEATURED -> ffPlugins
            PluginDirectoryType.NEW -> nnPlugins
            PluginDirectoryType.POPULAR -> ppPlugins
            PluginDirectoryType.SITE -> ssPlugins
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
        when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> {
                ssPlugins.value = NetworkResource.Loading(ssPlugins.value, loadMore)
                val payload = FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            PluginBrowserViewModel.PluginListType.FEATURED -> {
                ffPlugins.value = NetworkResource.Loading(ffPlugins.value, loadMore)
                val featuredPayload = FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            PluginBrowserViewModel.PluginListType.POPULAR -> {
                ppPlugins.value = NetworkResource.Loading(ppPlugins.value, loadMore)
                val popularPayload = FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            PluginBrowserViewModel.PluginListType.NEW -> {
                nnPlugins.value = NetworkResource.Loading(nnPlugins.value, loadMore)
                val newPayload = FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            PluginBrowserViewModel.PluginListType.SEARCH -> {
                val newStatus = if (loadMore) PluginListStatus.LOADING_MORE else PluginListStatus.FETCHING
                _searchPluginsListStatus.postValue(newStatus)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        return when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> ssPlugins.value.isFetching(loadMore)
            PluginBrowserViewModel.PluginListType.FEATURED -> ffPlugins.value.isFetching(loadMore)
            PluginBrowserViewModel.PluginListType.POPULAR -> ppPlugins.value.isFetching(loadMore)
            PluginBrowserViewModel.PluginListType.NEW -> nnPlugins.value.isFetching(loadMore)
            // We should always do the initial search because the string might have changed and it is
            // already optimized in submitSearch with a delay. Even though FluxC allows it, we don't do multiple
            // pages of search, so if we are trying to load more, we can ignore it
            PluginBrowserViewModel.PluginListType.SEARCH -> !loadMore
        }
    }

    fun loadMore(listType: PluginListType) {
        fetchPlugins(listType, true)
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onWPOrgPluginFetched(event: PluginStore.OnWPOrgPluginFetched) {
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
    @SuppressWarnings("unused")
    fun onPluginDirectoryFetched(event: PluginStore.OnPluginDirectoryFetched) {
        val liveData = getPluginListLiveData(event.type)

        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                    + event.error.type)
            liveData.value = NetworkResource.Error(liveData.value, event.error.message, event.loadMore)
        } else {
            successDirectory(event.type, event.canLoadMore)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onPluginDirectorySearched(event: PluginStore.OnPluginDirectorySearched) {
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
    @SuppressWarnings("unused")
    fun onSitePluginConfigured(event: PluginStore.OnSitePluginConfigured) {
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
    fun onSitePluginDeleted(event: PluginStore.OnSitePluginDeleted) {
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
    fun onSitePluginInstalled(event: PluginStore.OnSitePluginInstalled) {
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
    fun onSitePluginUpdated(event: PluginStore.OnSitePluginUpdated) {
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

    // Keeping the data up to date

    private fun updateAllPluginListsIfNecessary() {
//        val copiedSet = HashSet(updatedPluginSlugSet)
//        handler.postDelayed({
//            // Using the size of the set for comparison might fail since we clear the updatedPluginSlugSet
//            if (copiedSet == updatedPluginSlugSet) {
//                updateAllPluginListsWithNewPlugins(copiedSet)
//                updatedPluginSlugSet.clear()
//            }
//        }, 250)
    }
//
//    private fun updateAllPluginListsWithNewPlugins(updatedPluginSlugSet: Set<String>) {
//        if (updatedPluginSlugSet.isEmpty()) {
//            return
//        }
//        val newPluginMap = HashMap<String, ImmutablePluginModel>(updatedPluginSlugSet.size)
//        for (slug in updatedPluginSlugSet) {
//            val immutablePlugin = mPluginStore.getImmutablePluginBySlug(site!!, slug)
//            if (immutablePlugin != null) {
//                newPluginMap[slug] = immutablePlugin
//            }
//        }
//        // By combining all the updated plugins into one map, we can post a single update to the UI after changes are
//        // reflected
//        updatePluginListWithNewPlugin(_featuredPlugins, newPluginMap)
//        updatePluginListWithNewPlugin(_newPlugins, newPluginMap)
//        updatePluginListWithNewPlugin(_popularPlugins, newPluginMap)
//        updatePluginListWithNewPlugin(_searchResults, newPluginMap)
//
//        // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins can
//        // mess up the list. Also we care most about the Site Plugins and using the store to get the correct plugin
//        // information is much more reliable than any manual update we can make
//        reloadPluginDirectory(PluginDirectoryType.SITE)
//    }
//
//    private fun updatePluginListWithNewPlugin(mutableLiveData: MutableLiveData<List<ImmutablePluginModel>>,
//                                              newPluginMap: Map<String, ImmutablePluginModel>) {
//        val pluginList = mutableLiveData.value
//        if (pluginList == null || pluginList.isEmpty() || newPluginMap.isEmpty()) {
//            // Nothing to update
//            return
//        }
//        // When a site or wporg plugin is updated we need to update every occurrence of that item
//        val newList = ArrayList<ImmutablePluginModel>(pluginList.size)
//        var isChanged = false
//        for (immutablePlugin in pluginList) {
//            val slug = immutablePlugin.slug
//            val newPlugin = newPluginMap[slug]
//            if (newPlugin != null) {
//                // add new item
//                newList.add(newPlugin)
//                isChanged = true
//            } else {
//                // add old item
//                newList.add(immutablePlugin)
//            }
//        }
//        // Only update if the list is actually changed
//        if (isChanged) {
//            mutableLiveData.postValue(newList)
//        }
//    }

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
        return if (searchPluginsListStatus.value != PluginListStatus.DONE
                && searchPluginsListStatus.value != PluginListStatus.ERROR) {
            false
        } else searchResults.value == null || searchResults.value!!.isEmpty()
    }

    fun setTitle(title: String?) {
        _title.postValue(title)
    }
}
