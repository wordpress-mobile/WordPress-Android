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
import org.wordpress.android.models.ListNetworkResource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.FEATURED
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.NEW
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.POPULAR
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.SEARCH
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.SITE
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import javax.inject.Inject
import kotlin.properties.Delegates

private const val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"
private const val KEY_TITLE = "KEY_TITLE"

@WorkerThread
class PluginBrowserViewModel @Inject
constructor(private val mDispatcher: Dispatcher, private val mPluginStore: PluginStore) : ViewModel() {
    enum class PluginListType {
        SITE,
        FEATURED,
        POPULAR,
        NEW,
        SEARCH
    }

    private var isStarted = false

    private val handler = Handler()
    private val updatedPluginSlugSet = HashSet<String>()

    private val featuredPluginsResource = ListNetworkResource<ImmutablePluginModel>()
    private val newPluginsResource = ListNetworkResource<ImmutablePluginModel>()
    private val popularPluginsResource = ListNetworkResource<ImmutablePluginModel>()
    private val sitePluginsResource = ListNetworkResource<ImmutablePluginModel>()
    private val searchResultsResource = ListNetworkResource<ImmutablePluginModel>()

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

        fetchPlugins(SITE, false)
        fetchPlugins(FEATURED, false)
        fetchPlugins(POPULAR, false)
        fetchPlugins(NEW, false)

        isStarted = true
    }

    // Site & WPOrg plugin management

    private fun reloadPluginDirectory(directoryType: PluginDirectoryType) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(site!!, directoryType)
            when (directoryType) {
                PluginDirectoryType.FEATURED -> featuredPluginsResource.manuallyUpdateData(pluginList)
                PluginDirectoryType.NEW -> newPluginsResource.manuallyUpdateData(pluginList)
                PluginDirectoryType.POPULAR -> popularPluginsResource.manuallyUpdateData(pluginList)
                PluginDirectoryType.SITE -> sitePluginsResource.manuallyUpdateData(pluginList)
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
        when (listType) {
            SITE -> {
                sitePluginsResource.fetching(loadMore)
                val payload = FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            FEATURED -> {
                featuredPluginsResource.fetching(loadMore)
                val featuredPayload = FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            POPULAR -> {
                popularPluginsResource.fetching(loadMore)
                val popularPayload = FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            NEW -> {
                newPluginsResource.fetching(loadMore)
                val newPayload = FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            SEARCH -> {
                searchResultsResource.fetching(loadMore)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        return getListNetworkResourceForListType(listType).shouldFetch(loadMore)
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
        val listResource = getListNetworkResourceForDirectoryType(event.type)
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                    + event.error.type)
            listResource.fetchError(event.error.message)
            return
        }
        listResource.fetchedSuccessfully(mPluginStore.getPluginDirectory(site!!, event.type), event.loadMore)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onPluginDirectorySearched(event: PluginStore.OnPluginDirectorySearched) {
        if (searchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while searching the plugin directory")
            searchResultsResource.fetchError(event.error.message)
            return
        }
        searchResultsResource.fetchedSuccessfully(event.plugins, false)
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
        updatePluginListWithNewPlugin(featuredPluginsResource, newPluginMap)
        updatePluginListWithNewPlugin(newPluginsResource, newPluginMap)
        updatePluginListWithNewPlugin(popularPluginsResource, newPluginMap)
        updatePluginListWithNewPlugin(searchResultsResource, newPluginMap)

        // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins can
        // mess up the list. Also we care most about the Site Plugins and using the store to get the correct plugin
        // information is much more reliable than any manual update we can make
        reloadPluginDirectory(PluginDirectoryType.SITE)
    }

    private fun updatePluginListWithNewPlugin(pluginListNetworkResource: ListNetworkResource<ImmutablePluginModel>,
                                              newPluginMap: Map<String, ImmutablePluginModel>) {
        val pluginList = pluginListNetworkResource.data.value
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
            pluginListNetworkResource.manuallyUpdateData(newList)
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
            searchResultsResource.manuallyUpdateData(ArrayList())

            if (shouldSearch) {
                fetchPlugins(SEARCH, false)
            } else {
                // Due to the query being changed after the last fetch, the status won't ever be updated, so we need
                // to manually do it. Consider the following case:
                // 1. Search the plugins for "contact" which will change the status to FETCHING
                // 2. Before the fetch completes delete the text
                // 3. In `onPluginDirectorySearched` the result will be ignored, because the query changed, but it won't
                // be triggered again, because another fetch didn't happen (due to query being empty)
                // 4. The status will be stuck in FETCHING until another search occurs. The following reset fixes the
                // problem.
                searchResultsResource.resetStatus()
            }
        }
    }

    fun shouldShowEmptySearchResultsView(): Boolean {
        // Search query is less than 2 characters
        if (!shouldSearch) {
            return false
        }
        // Only show empty view if content is empty, we are not fetching new data and no errors occurred
        return searchResultsResource.isEmpty()
                && !searchResultsResource.isFetchingFirstPage()
                && !searchResultsResource.isError()
    }

    fun setTitle(title: String?) {
        _title.postValue(title)
    }

    fun getPluginsForListType(listType: PluginListType): List<ImmutablePluginModel>? {
        return when (listType) {
            SITE -> sitePluginsResource.data.value
            FEATURED -> featuredPluginsResource.data.value
            POPULAR -> popularPluginsResource.data.value
            NEW -> newPluginsResource.data.value
            SEARCH -> searchResultsResource.data.value
        }
    }

    private fun getListNetworkResourceForDirectoryType(directoryType: PluginDirectoryType):
            ListNetworkResource<ImmutablePluginModel> {
        return when (directoryType) {
            PluginDirectoryType.FEATURED -> featuredPluginsResource
            PluginDirectoryType.NEW -> newPluginsResource
            PluginDirectoryType.POPULAR -> popularPluginsResource
            PluginDirectoryType.SITE -> sitePluginsResource
        }
    }

    private fun getListNetworkResourceForListType(listType: PluginListType): ListNetworkResource<ImmutablePluginModel> {
        return when (listType) {
            SITE -> sitePluginsResource
            FEATURED -> featuredPluginsResource
            POPULAR -> popularPluginsResource
            NEW -> newPluginsResource
            SEARCH -> searchResultsResource
        }
    }

    // Getters for LiveData properties

    val sitePlugins
        get() = sitePluginsResource.data
    val featuredPlugins
        get() = featuredPluginsResource.data
    val popularPlugins
        get() = popularPluginsResource.data
    val newPlugins
        get() = newPluginsResource.data
    val searchResults
        get() = searchResultsResource.data

    val sitePluginsStatus
        get() = sitePluginsResource.status
    val featuredPluginsStatus
        get() = featuredPluginsResource.status
    val popularPluginsStatus
        get() = popularPluginsResource.status
    val newPluginsStatus
        get() = newPluginsResource.status
    val searchResultsStatus
        get() = searchResultsResource.status

    fun isSitePluginsEmpty() = sitePluginsResource.isEmpty()
}
