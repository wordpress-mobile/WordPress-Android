package org.wordpress.android.viewmodel.plugins

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.ui.ListDiffCallback
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel.PluginListType.FEATURED
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel.PluginListType.NEW
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel.PluginListType.POPULAR
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel.PluginListType.SEARCH
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel.PluginListType.SITE
import javax.inject.Inject
import kotlin.properties.Delegates

private const val KEY_SEARCH_QUERY = "KEY_SEARCH_QUERY"
private const val KEY_TITLE = "KEY_TITLE"

typealias PluginListState = ListState<ImmutablePluginModel>

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

    private val handler = Handler(Looper.getMainLooper())

    private val _featuredPluginsLiveData = MutableLiveData<PluginListState>()
    private val _popularPluginsLiveData = MutableLiveData<PluginListState>()
    private val _newPluginsLiveData = MutableLiveData<PluginListState>()
    private val _sitePluginsLiveData = MutableLiveData<PluginListState>()
    private val _searchResultsLiveData = MutableLiveData<PluginListState>()

    val featuredPluginsLiveData: LiveData<PluginListState>
        get() = _featuredPluginsLiveData

    val popularPluginsLiveData: LiveData<PluginListState>
        get() = _popularPluginsLiveData

    val newPluginsLiveData: LiveData<PluginListState>
        get() = _newPluginsLiveData

    val sitePluginsLiveData: LiveData<PluginListState>
        get() = _sitePluginsLiveData

    val searchResultsLiveData: LiveData<PluginListState>
        get() = _searchResultsLiveData

    private var featuredPlugins: ListState<ImmutablePluginModel>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _featuredPluginsLiveData.postValue(new)
            }
    private var popularPlugins: ListState<ImmutablePluginModel>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _popularPluginsLiveData.postValue(new)
            }
    private var newPlugins: ListState<ImmutablePluginModel>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _newPluginsLiveData.postValue(new)
            }
    private var sitePlugins: ListState<ImmutablePluginModel>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _sitePluginsLiveData.postValue(new)
            }
    private var searchResults: ListState<ImmutablePluginModel>
            by Delegates.observable(ListState.Init()) { _, _, new ->
                _searchResultsLiveData.postValue(new)
            }

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    lateinit var site: SiteModel

    var searchQuery: String by Delegates.observable("") { _, oldValue, newValue ->
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
        searchQuery = requireNotNull(savedInstanceState.getString(KEY_SEARCH_QUERY))
        setTitle(savedInstanceState.getString(KEY_TITLE))
    }

    fun start() {
        if (isStarted) {
            return
        }
        updateListStateToReady(PluginDirectoryType.FEATURED)
        updateListStateToReady(PluginDirectoryType.NEW)
        updateListStateToReady(PluginDirectoryType.POPULAR)
        updateListStateToReady(PluginDirectoryType.SITE)

        fetchPlugins(SITE, false)
        fetchPlugins(FEATURED, false)
        fetchPlugins(POPULAR, false)
        fetchPlugins(NEW, false)

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
                    same = old.slug == new.slug &&
                            old.displayName == new.displayName &&
                            old.authorName == new.authorName &&
                            old.icon == new.icon &&
                            old.isInstalled == new.isInstalled &&
                            old.isActive == new.isActive &&
                            old.isAutoUpdateEnabled == new.isAutoUpdateEnabled &&
                            old.installedVersion == new.installedVersion &&
                            old.wpOrgPluginVersion == new.wpOrgPluginVersion &&
                            old.averageStarRating == new.averageStarRating
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
            SITE -> {
                sitePlugins = ListState.Loading(sitePlugins, loadMore)
                val payload = FetchPluginDirectoryPayload(PluginDirectoryType.SITE, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
            }
            FEATURED -> {
                featuredPlugins = ListState.Loading(featuredPlugins, loadMore)
                val featuredPayload = FetchPluginDirectoryPayload(PluginDirectoryType.FEATURED, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(featuredPayload))
            }
            POPULAR -> {
                popularPlugins = ListState.Loading(popularPlugins, loadMore)
                val popularPayload = FetchPluginDirectoryPayload(PluginDirectoryType.POPULAR, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(popularPayload))
            }
            NEW -> {
                newPlugins = ListState.Loading(newPlugins, loadMore)
                val newPayload = FetchPluginDirectoryPayload(PluginDirectoryType.NEW, site, loadMore)
                mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(newPayload))
            }
            SEARCH -> {
                searchResults = ListState.Loading(searchResults, loadMore)
                val searchPayload = PluginStore.SearchPluginDirectoryPayload(site, searchQuery, 1)
                mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
            }
        }
    }

    private fun shouldFetchPlugins(listType: PluginListType, loadMore: Boolean): Boolean {
        if (listType == SITE && SiteUtils.isNonAtomicBusinessPlanSite(site)) {
            return false
        }
        return when (listType) {
            SITE -> sitePlugins.shouldFetch(loadMore)
            FEATURED -> featuredPlugins.shouldFetch(loadMore)
            POPULAR -> popularPlugins.shouldFetch(loadMore)
            NEW -> newPlugins.shouldFetch(loadMore)
            // We should always do the initial search because the string might have changed and it is
            // already optimized in submitSearch with a delay. Even though FluxC allows it, we don't do multiple
            // pages of search, so if we are trying to load more, we can ignore it
            SEARCH -> !loadMore
        }
    }

    // Network Callbacks

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWPOrgPluginFetched(event: OnWPOrgPluginFetched) {
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the wporg plugin with type: " + event.error.type)
            return
        }
        updateAllPluginListsForSlug(event.pluginSlug)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPluginDirectoryFetched(event: OnPluginDirectoryFetched) {
        if (event.isError) {
            AppLog.e(
                T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": " +
                        event.error.type
            )
            updateListStateToError(event.type, event.error.message)
        } else {
            updateListStateToSuccess(event.type, event.canLoadMore)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPluginDirectorySearched(event: OnPluginDirectorySearched) {
        if (searchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while searching the plugin directory")
            searchResults = ListState.Error(searchResults, event.error.message)
            return
        }
        searchResults = ListState.Success(event.plugins, false) // Disable pagination for search
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginConfigured(event: OnSitePluginConfigured) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginDeleted(event: OnSitePluginDeleted) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginInstalled(event: OnSitePluginInstalled) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSitePluginUpdated(event: OnSitePluginUpdated) {
        if (event.isError) {
            // The error should be handled wherever the action has been triggered from (probably PluginDetailActivity)
            return
        }
        updateAllPluginListsForSlug(event.slug)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            // The error should be safe to ignore since we are not triggering the action and there is nothing we need
            // to do about it
            return
        }

        site = if (site.isUsingWpComRestApi) {
            requireNotNull(mSiteStore.getSiteBySiteId(site.siteId))
        } else {
            requireNotNull(mSiteStore.getSiteByLocalId(site.id))
        }
    }

    // Keeping the data up to date

    private fun updateAllPluginListsForSlug(slug: String?) {
        mPluginStore.getImmutablePluginBySlug(site, slug)?.let { updatedPlugin ->
            val transformFunc: (List<ImmutablePluginModel>) -> List<ImmutablePluginModel> = { list ->
                list.map { currentPlugin ->
                    if (currentPlugin.slug == slug) updatedPlugin else currentPlugin
                }
            }
            featuredPlugins = featuredPlugins.transform(transformFunc)
            newPlugins = newPlugins.transform(transformFunc)
            searchResults = searchResults.transform(transformFunc)
            popularPlugins = popularPlugins.transform(transformFunc)

            sitePlugins = sitePlugins.transform { list ->
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

    private fun submitSearch(query: String, delayed: Boolean) {
        if (delayed) {
            handler.postDelayed({
                if (query == searchQuery) {
                    submitSearch(query, false)
                }
            }, 250)
        } else {
            searchResults = ListState.Ready(ArrayList())

            if (shouldSearch) {
                fetchPlugins(SEARCH, false)
            }
        }
    }

    fun shouldShowEmptySearchResultsView(): Boolean {
        // Search query is less than 2 characters
        if (!shouldSearch) {
            return false
        }
        return when (searchResults) {
            is ListState.Success, is ListState.Error -> searchResults.data.isEmpty()
            else -> false
        }
    }

    fun setTitle(title: String?) {
        _title.postValue(title ?: "")
    }

    // ListState Helpers

    private fun updateListStateToReady(directoryType: PluginDirectoryType) {
        val pluginList = mPluginStore.getPluginDirectory(site, directoryType)
        when (directoryType) {
            PluginDirectoryType.FEATURED -> featuredPlugins = ListState.Ready(pluginList)
            PluginDirectoryType.NEW -> newPlugins = ListState.Ready(pluginList)
            PluginDirectoryType.POPULAR -> popularPlugins = ListState.Ready(pluginList)
            PluginDirectoryType.SITE -> sitePlugins = ListState.Ready(pluginList)
        }
    }

    private fun updateListStateToSuccess(directoryType: PluginDirectoryType, canLoadMore: Boolean) {
        val pluginList = mPluginStore.getPluginDirectory(site, directoryType)
        when (directoryType) {
            PluginDirectoryType.FEATURED -> featuredPlugins = ListState.Success(pluginList, canLoadMore)
            PluginDirectoryType.NEW -> newPlugins = ListState.Success(pluginList, canLoadMore)
            PluginDirectoryType.POPULAR -> popularPlugins = ListState.Success(pluginList, canLoadMore)
            PluginDirectoryType.SITE -> sitePlugins = ListState.Success(pluginList, canLoadMore)
        }
    }

    private fun updateListStateToError(directoryType: PluginDirectoryType, errorMessage: String?) {
        when (directoryType) {
            PluginDirectoryType.FEATURED -> featuredPlugins = ListState.Error(featuredPlugins, errorMessage)
            PluginDirectoryType.NEW -> newPlugins = ListState.Error(newPlugins, errorMessage)
            PluginDirectoryType.POPULAR -> popularPlugins = ListState.Error(popularPlugins, errorMessage)
            PluginDirectoryType.SITE -> sitePlugins = ListState.Error(sitePlugins, errorMessage)
        }
    }
}
