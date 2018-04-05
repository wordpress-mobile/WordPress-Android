package org.wordpress.android.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.os.Handler
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
import org.wordpress.android.models.networkresource.ListNetworkResource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import kotlin.properties.Delegates

private const val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"
private const val KEY_TITLE = "KEY_TITLE"

typealias PluginListNetworkResource = ListNetworkResource<ImmutablePluginModel>

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

    val featuredLiveData = MutableLiveData<PluginListNetworkResource>()
    val popularLiveData = MutableLiveData<PluginListNetworkResource>()
    val newLiveData = MutableLiveData<PluginListNetworkResource>()
    val siteLiveData = MutableLiveData<PluginListNetworkResource>()
    val searchLiveData = MutableLiveData<ListNetworkResource<ImmutablePluginModel>>()

    private var listFeatured: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                featuredLiveData.postValue(new)
            }
    private var listPopular: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                popularLiveData.postValue(new)
            }
    private var listNew: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                newLiveData.postValue(new)
            }
    private var listSite: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                siteLiveData.postValue(new)
            }
    private var listSearch: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                searchLiveData.postValue(new)
            }

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
            when (directoryType) {
                PluginDirectoryType.FEATURED -> listFeatured = ListNetworkResource.Ready(pluginList)
                PluginDirectoryType.NEW -> listNew = ListNetworkResource.Ready(pluginList)
                PluginDirectoryType.POPULAR -> listPopular = ListNetworkResource.Ready(pluginList)
                PluginDirectoryType.SITE -> listSite = ListNetworkResource.Ready(pluginList)
            }
        }
    }

    private fun successDirectory(directoryType: PluginDirectoryType, canLoadMore: Boolean) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(it, directoryType)
            when (directoryType) {
                PluginDirectoryType.FEATURED -> listFeatured = ListNetworkResource.Success(pluginList, canLoadMore)
                PluginDirectoryType.NEW -> listNew = ListNetworkResource.Success(pluginList, canLoadMore)
                PluginDirectoryType.POPULAR -> listPopular = ListNetworkResource.Success(pluginList, canLoadMore)
                PluginDirectoryType.SITE -> listSite = ListNetworkResource.Success(pluginList, canLoadMore)
            }
        }
    }

    private fun errorDirectory(directoryType: PluginDirectoryType, errorMessage: String?, loadMore: Boolean) {
        when (directoryType) {
            PluginDirectoryType.FEATURED ->
                listFeatured = ListNetworkResource.Error(listFeatured, errorMessage, loadMore)
            PluginDirectoryType.NEW ->
                listNew = ListNetworkResource.Error(listNew, errorMessage, loadMore)
            PluginDirectoryType.POPULAR ->
                listPopular = ListNetworkResource.Error(listPopular, errorMessage, loadMore)
            PluginDirectoryType.SITE ->
                listSite = ListNetworkResource.Error(listSite, errorMessage, loadMore)
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
                listSite = ListNetworkResource.Loading(listSite, loadMore)
                val payload = FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            PluginBrowserViewModel.PluginListType.FEATURED -> {
                listFeatured = ListNetworkResource.Loading(listFeatured, loadMore)
                val featuredPayload = FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            PluginBrowserViewModel.PluginListType.POPULAR -> {
                listPopular = ListNetworkResource.Loading(listPopular, loadMore)
                val popularPayload = FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            PluginBrowserViewModel.PluginListType.NEW -> {
                listNew = ListNetworkResource.Loading(listNew, loadMore)
                val newPayload = FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            PluginBrowserViewModel.PluginListType.SEARCH -> {
                listSearch = ListNetworkResource.Loading(listSearch, loadMore)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        return when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> listSite.shouldFetch(loadMore)
            PluginBrowserViewModel.PluginListType.FEATURED -> listFeatured.shouldFetch(loadMore)
            PluginBrowserViewModel.PluginListType.POPULAR -> listPopular.shouldFetch(loadMore)
            PluginBrowserViewModel.PluginListType.NEW -> listNew.shouldFetch(loadMore)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onWPOrgPluginFetched(event: PluginStore.OnWPOrgPluginFetched) {
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the wporg plugin with type: " + event.error.type)
            return
        }
        updateAllPluginListsForSlug(event.pluginSlug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPluginDirectoryFetched(event: PluginStore.OnPluginDirectoryFetched) {
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                    + event.error.type)
            errorDirectory(event.type, event.error.message, event.loadMore)
        } else {
            successDirectory(event.type, event.canLoadMore)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPluginDirectorySearched(event: PluginStore.OnPluginDirectorySearched) {
        if (searchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while searching the plugin directory")
            listSearch = ListNetworkResource.Error(listSearch, event.error.message)
            return
        }
        listSearch = ListNetworkResource.Success(event.plugins, false) // Disable pagination for search
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginConfigured(event: PluginStore.OnSitePluginConfigured) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginDeleted(event: PluginStore.OnSitePluginDeleted) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginInstalled(event: PluginStore.OnSitePluginInstalled) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginUpdated(event: PluginStore.OnSitePluginUpdated) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    // Keeping the data up to date

    private fun updateAllPluginListsForSlug(slug: String?) {
        site?.let { site ->
            mPluginStore.getImmutablePluginBySlug(site, slug)?.let { updatedPlugin ->
                val compare: (ImmutablePluginModel, ImmutablePluginModel) -> Boolean = { first, second ->
                    first.slug.equals(second.slug)
                }
                listFeatured = listFeatured.updated(updatedPlugin, compare)
                listNew = listNew.updated(updatedPlugin, compare)
                listSearch = listSearch.updated(updatedPlugin, compare)
                listPopular = listPopular.updated(updatedPlugin, compare)

                // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins
                // can mess up the list. Also we care most about the Site Plugins and using the store to get the correct
                // plugin information is much more reliable than any manual update we can make
            }
        }
    }

    private fun submitSearch(query: String, delayed: Boolean) {
        if (delayed) {
            handler.postDelayed({
                if (query == searchQuery) {
                    submitSearch(query, false)
                }
            }, 250)
        } else {
            listSearch = ListNetworkResource.Ready(ArrayList())

            if (shouldSearch) {
                fetchPlugins(PluginListType.SEARCH, false)
            }
        }
    }

    fun shouldShowEmptySearchResultsView(): Boolean {
        // Search query is less than 2 characters
        if (!shouldSearch) {
            return false
        }
        return when (listSearch) {
            is ListNetworkResource.Success, is ListNetworkResource.Error -> listSearch.data.isEmpty()
            else -> false
        }
    }

    fun setTitle(title: String?) {
        _title.postValue(title)
    }
}
