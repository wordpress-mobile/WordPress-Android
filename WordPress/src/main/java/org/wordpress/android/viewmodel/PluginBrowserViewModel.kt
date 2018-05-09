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
import org.wordpress.android.ui.ListDiffCallback
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

typealias PluginListNetworkResource = ListNetworkResource<ImmutablePluginModel>

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

    private var isStarted = false

    private val handler = Handler()

    private val _featuredPluginsLiveData = MutableLiveData<PluginListNetworkResource>()
    private val _popularPluginsLiveData = MutableLiveData<PluginListNetworkResource>()
    private val _newPluginsLiveData = MutableLiveData<PluginListNetworkResource>()
    private val _sitePluginsLiveData = MutableLiveData<PluginListNetworkResource>()
    private val _searchResultsLiveData = MutableLiveData<PluginListNetworkResource>()

    val featuredPluginsLiveData: LiveData<PluginListNetworkResource>
        get() = _featuredPluginsLiveData

    val popularPluginsLiveData: LiveData<PluginListNetworkResource>
        get() = _popularPluginsLiveData

    val newPluginsLiveData: LiveData<PluginListNetworkResource>
        get() = _newPluginsLiveData

    val sitePluginsLiveData: LiveData<PluginListNetworkResource>
        get() = _sitePluginsLiveData

    val searchResultsLiveData: LiveData<PluginListNetworkResource>
        get() = _searchResultsLiveData

    private var featuredPlugins: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                _featuredPluginsLiveData.postValue(new)
            }
    private var popularPlugins: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                _popularPluginsLiveData.postValue(new)
            }
    private var newPlugins: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                _newPluginsLiveData.postValue(new)
            }
    private var sitePlugins: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                _sitePluginsLiveData.postValue(new)
            }
    private var searchResults: ListNetworkResource<ImmutablePluginModel>
            by Delegates.observable(ListNetworkResource.Init()) { _, _, new ->
                _searchResultsLiveData.postValue(new)
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
        readyListNetworkResource(PluginDirectoryType.FEATURED)
        readyListNetworkResource(PluginDirectoryType.NEW)
        readyListNetworkResource(PluginDirectoryType.POPULAR)
        readyListNetworkResource(PluginDirectoryType.SITE)

        fetchPlugins(PluginListType.SITE, false)
        fetchPlugins(PluginListType.FEATURED, false)
        fetchPlugins(PluginListType.POPULAR, false)
        fetchPlugins(PluginListType.NEW, false)

        isStarted = true
    }

    // Actions

    fun getDiffCallback(oldList: List<ImmutablePluginModel>, newList: List<ImmutablePluginModel>):
            ListDiffCallback<ImmutablePluginModel> {
        val areItemsTheSame: (ImmutablePluginModel?, ImmutablePluginModel?) -> Boolean = { old, new ->
            old?.slug == new?.slug
        }

        val areContentsTheSame: (ImmutablePluginModel?, ImmutablePluginModel?) -> Boolean = { old, new ->
            var same = false
            old?.let {
                new?.let {
                    same = old.slug == new.slug
                            && old.displayName == new.displayName
                            && old.authorName == new.authorName
                            && old.icon == new.icon
                            && old.isInstalled == new.isInstalled
                            && old.isActive == new.isActive
                            && old.isAutoUpdateEnabled == new.isAutoUpdateEnabled
                            && old.installedVersion == new.installedVersion
                            && old.wpOrgPluginVersion == new.wpOrgPluginVersion
                            && old.averageStarRating == new.averageStarRating
                }
            }
            same
        }
        return ListDiffCallback(oldList, newList, areItemsTheSame, areContentsTheSame)
    }

    fun loadMore(listType: PluginListType) {
        fetchPlugins(listType, true)
    }

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
                sitePlugins = ListNetworkResource.Loading(sitePlugins, loadMore)
                val payload = FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            PluginBrowserViewModel.PluginListType.FEATURED -> {
                featuredPlugins = ListNetworkResource.Loading(featuredPlugins, loadMore)
                val featuredPayload = FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            PluginBrowserViewModel.PluginListType.POPULAR -> {
                popularPlugins = ListNetworkResource.Loading(popularPlugins, loadMore)
                val popularPayload = FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            PluginBrowserViewModel.PluginListType.NEW -> {
                newPlugins = ListNetworkResource.Loading(newPlugins, loadMore)
                val newPayload = FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            PluginBrowserViewModel.PluginListType.SEARCH -> {
                searchResults = ListNetworkResource.Loading(searchResults, loadMore)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        if (listType == PluginListType.SITE && SiteUtils.isNonAtomicBusinessPlanSite(site)) {
            return false
        }
        return when (listType) {
            PluginBrowserViewModel.PluginListType.SITE -> sitePlugins.shouldFetch(loadMore)
            PluginBrowserViewModel.PluginListType.FEATURED -> featuredPlugins.shouldFetch(loadMore)
            PluginBrowserViewModel.PluginListType.POPULAR -> popularPlugins.shouldFetch(loadMore)
            PluginBrowserViewModel.PluginListType.NEW -> newPlugins.shouldFetch(loadMore)
            // We should always do the initial search because the string might have changed and it is
            // already optimized in submitSearch with a delay. Even though FluxC allows it, we don't do multiple
            // pages of search, so if we are trying to load more, we can ignore it
            PluginBrowserViewModel.PluginListType.SEARCH -> !loadMore
        }
    }

    // Network Callbacks

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onWPOrgPluginFetched(event: OnWPOrgPluginFetched) {
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the wporg plugin with type: " + event.error.type)
            return
        }
        updateAllPluginListsForSlug(event.pluginSlug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPluginDirectoryFetched(event: OnPluginDirectoryFetched) {
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                    + event.error.type)
            errorListNetworkResource(event.type, event.error.message)
        } else {
            successListNetworkResource(event.type, event.canLoadMore)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onPluginDirectorySearched(event: OnPluginDirectorySearched) {
        if (searchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while searching the plugin directory")
            searchResults = ListNetworkResource.Error(searchResults, event.error.message)
            return
        }
        searchResults = ListNetworkResource.Success(event.plugins, false) // Disable pagination for search
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginConfigured(event: OnSitePluginConfigured) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginDeleted(event: OnSitePluginDeleted) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginInstalled(event: OnSitePluginInstalled) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onSitePluginUpdated(event: OnSitePluginUpdated) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
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

    private fun updateAllPluginListsForSlug(slug: String?) {
        site?.let { site ->
            mPluginStore.getImmutablePluginBySlug(site, slug)?.let { updatedPlugin ->
                val transform: (List<ImmutablePluginModel>) -> List<ImmutablePluginModel> = { list ->
                    list.map { currentPlugin ->
                        if (currentPlugin.slug == slug) updatedPlugin else currentPlugin
                    }
                }
                featuredPlugins = featuredPlugins.getTransformedListNetworkResource(transform)
                newPlugins = newPlugins.getTransformedListNetworkResource(transform)
                searchResults = searchResults.getTransformedListNetworkResource(transform)
                popularPlugins = popularPlugins.getTransformedListNetworkResource(transform)

                sitePlugins = sitePlugins.getTransformedListNetworkResource { list ->
                    if (!updatedPlugin.isInstalled) {
                        list.filter { it.slug != slug }
                    } else if (list.none { it.slug == slug }) {
                        list.plus(updatedPlugin).sortedBy { it.displayName }
                    } else {
                        list.map { if (it.slug == slug) updatedPlugin else it }
                    }
                }
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
            searchResults = ListNetworkResource.Ready(ArrayList())

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
        return when (searchResults) {
            is ListNetworkResource.Success, is ListNetworkResource.Error -> searchResults.data.isEmpty()
            else -> false
        }
    }

    fun setTitle(title: String?) {
        _title.postValue(title)
    }

    // ListNetworkResource State Management Helpers

    private fun readyListNetworkResource(directoryType: PluginDirectoryType) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(it, directoryType)
            when (directoryType) {
                PluginDirectoryType.FEATURED -> featuredPlugins = ListNetworkResource.Ready(pluginList)
                PluginDirectoryType.NEW -> newPlugins = ListNetworkResource.Ready(pluginList)
                PluginDirectoryType.POPULAR -> popularPlugins = ListNetworkResource.Ready(pluginList)
                PluginDirectoryType.SITE -> sitePlugins = ListNetworkResource.Ready(pluginList)
            }
        }
    }

    private fun successListNetworkResource(directoryType: PluginDirectoryType, canLoadMore: Boolean) {
        site?.let {
            val pluginList = mPluginStore.getPluginDirectory(it, directoryType)
            when (directoryType) {
                PluginDirectoryType.FEATURED -> featuredPlugins = ListNetworkResource.Success(pluginList, canLoadMore)
                PluginDirectoryType.NEW -> newPlugins = ListNetworkResource.Success(pluginList, canLoadMore)
                PluginDirectoryType.POPULAR -> popularPlugins = ListNetworkResource.Success(pluginList, canLoadMore)
                PluginDirectoryType.SITE -> sitePlugins = ListNetworkResource.Success(pluginList, canLoadMore)
            }
        }
    }

    private fun errorListNetworkResource(directoryType: PluginDirectoryType, errorMessage: String?) {
        when (directoryType) {
            PluginDirectoryType.FEATURED -> featuredPlugins = ListNetworkResource.Error(featuredPlugins, errorMessage)
            PluginDirectoryType.NEW -> newPlugins = ListNetworkResource.Error(newPlugins, errorMessage)
            PluginDirectoryType.POPULAR -> popularPlugins = ListNetworkResource.Error(popularPlugins, errorMessage)
            PluginDirectoryType.SITE -> sitePlugins = ListNetworkResource.Error(sitePlugins, errorMessage)
        }
    }
}
