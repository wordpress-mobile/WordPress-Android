package org.wordpress.android.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.support.annotation.WorkerThread
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
import org.wordpress.android.models.networkresource.IListNetworkResource
import org.wordpress.android.models.networkresource.ISearchListNetworkResource
import org.wordpress.android.models.networkresource.ListNetworkResource
import org.wordpress.android.models.networkresource.SearchListNetworkResource
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.FEATURED
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.NEW
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.POPULAR
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.SEARCH
import org.wordpress.android.viewmodel.PluginBrowserViewModel.PluginListType.SITE
import javax.inject.Inject

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

//    private val handler = Handler()
//    private val updatedPluginSlugSet = HashSet<String>()

    private val _featuredPlugins = ListNetworkResource<ImmutablePluginModel> { loadMore ->
        fetchPluginDirectory(PluginDirectoryType.FEATURED, loadMore)
    }
    private val _newPlugins = ListNetworkResource<ImmutablePluginModel> { loadMore ->
        fetchPluginDirectory(PluginDirectoryType.NEW, loadMore)
    }
    private val _popularPlugins = ListNetworkResource<ImmutablePluginModel> { loadMore ->
        fetchPluginDirectory(PluginDirectoryType.POPULAR, loadMore)
    }
    private val _sitePlugins = ListNetworkResource<ImmutablePluginModel> { loadMore ->
        fetchPluginDirectory(PluginDirectoryType.SITE, loadMore)
    }
    private val _searchResults = SearchListNetworkResource<ImmutablePluginModel> { searchQuery, loadMore ->
        searchPlugins(searchQuery, loadMore)
    }

    val featuredPlugins: IListNetworkResource<ImmutablePluginModel>
        get() = _featuredPlugins
    val newPlugins: IListNetworkResource<ImmutablePluginModel>
        get() = _newPlugins
    val popularPlugins: IListNetworkResource<ImmutablePluginModel>
        get() = _popularPlugins
    val sitePlugins: IListNetworkResource<ImmutablePluginModel>
        get() = _sitePlugins
    val searchResults: ISearchListNetworkResource<ImmutablePluginModel>
        get() = _searchResults

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    var site: SiteModel? = null
        set(value) {
            requireNotNull(value) {
                "Site shouldn't be set to null, make sure it's properly saved and retrieved from Bundle"
            }
            value?.let {
                val wasNull = field == null
                field = it
                if (wasNull) {
                    _featuredPlugins.ready(mPluginStore.getPluginDirectory(it, PluginDirectoryType.FEATURED))
                    _newPlugins.ready(mPluginStore.getPluginDirectory(it, PluginDirectoryType.NEW))
                    _popularPlugins.ready(mPluginStore.getPluginDirectory(it, PluginDirectoryType.POPULAR))
                    _sitePlugins.ready(mPluginStore.getPluginDirectory(it, PluginDirectoryType.SITE))
                    _searchResults.ready(ArrayList())
                }
            }
        }

    init {
        mDispatcher.register(this)
    }

    override fun onCleared() {
        mDispatcher.unregister(this)
        super.onCleared()
    }

    fun writeToBundle(outState: Bundle) {
        outState.putSerializable(WordPress.SITE, site)
        outState.putString(KEY_SEARCH_QUERY, _searchResults.searchQuery)
        outState.putString(KEY_TITLE, _title.value)
    }

    fun readFromBundle(savedInstanceState: Bundle) {
        // If the site is not null it means this is due to config change and all the data survived, so we can safely
        // ignore the bundle
        if (site == null) {
            site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
            setSearchQuery(savedInstanceState.getString(KEY_SEARCH_QUERY))
            setTitle(savedInstanceState.getString(KEY_TITLE))
        }
    }

    fun setSearchQuery(searchQuery: String) {
        _searchResults.searchQuery = searchQuery
    }

    // Network Requests

    private fun fetchPluginDirectory(directoryType: PluginDirectoryType, loadMore: Boolean) {
        site?.let {
            val payload = FetchPluginDirectoryPayload(directoryType, it, loadMore)
            mDispatcher.dispatch(PluginActionBuilder.newFetchPluginDirectoryAction(payload))
        }
    }

    // TODO: Expose the page parameter from SearchListNetworkResource instead of / in addition to `loadMore`
    private fun searchPlugins(searchQuery: String, loadMore: Boolean) {
        site?.let {
            val searchPayload = PluginStore.SearchPluginDirectoryPayload(it, searchQuery, 1)
            mDispatcher.dispatch(PluginActionBuilder.newSearchPluginDirectoryAction(searchPayload))
        }
    }

    fun loadMore(listType: PluginListType) {
        getListNetworkResourceForListType(listType).loadMore()
    }

    fun pullToRefresh(listType: PluginListType) {
        getListNetworkResourceForListType(listType).pullToRefresh()
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
//        if (!event.pluginSlug.isNullOrEmpty() && updatedPluginSlugSet.add(event.pluginSlug)) {
//            updateAllPluginListsIfNecessary()
//        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onPluginDirectoryFetched(event: PluginStore.OnPluginDirectoryFetched) {
        val listResource = getListNetworkResourceForDirectoryType(event.type)
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while fetching the plugin directory " + event.type + ": "
                    + event.error.type)
            listResource.error(event.error.message, event.loadMore)
            return
        }
        site?.let {
            listResource.success(mPluginStore.getPluginDirectory(it, event.type), event.canLoadMore)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onPluginDirectorySearched(event: PluginStore.OnPluginDirectorySearched) {
        if (_searchResults.searchQuery != event.searchTerm) {
            return
        }
        if (event.isError) {
            AppLog.e(T.PLUGINS, "An error occurred while searching the plugin directory")
            _searchResults.error(event.error.message, false)
            return
        }
        // Disable loading more pages for search results
        _searchResults.success(event.plugins, false)
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
//        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
//            updateAllPluginListsIfNecessary()
//        }
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
//        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
//            updateAllPluginListsIfNecessary()
//        }
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
//        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
//            updateAllPluginListsIfNecessary()
//        }
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
//        if (!TextUtils.isEmpty(event.slug) && updatedPluginSlugSet.add(event.slug)) {
//            updateAllPluginListsIfNecessary()
//        }
    }

    // Keeping the data up to date

//    private fun updateAllPluginListsIfNecessary() {
//        val copiedSet = HashSet(updatedPluginSlugSet)
//        handler.postDelayed({
//            // Using the size of the set for comparison might fail since we clear the updatedPluginSlugSet
//            if (copiedSet == updatedPluginSlugSet) {
//                updateAllPluginListsWithNewPlugins(copiedSet)
//                updatedPluginSlugSet.clear()
//            }
//        }, 250)
//    }
//
//    private fun updateAllPluginListsWithNewPlugins(updatedPluginSlugSet: Set<String>) {
//        if (updatedPluginSlugSet.isEmpty()) {
//            return
//        }
//        site?.let {
//            val newPluginMap = HashMap<String, ImmutablePluginModel>(updatedPluginSlugSet.size)
//            for (slug in updatedPluginSlugSet) {
//                val immutablePlugin = mPluginStore.getImmutablePluginBySlug(it, slug)
//                if (immutablePlugin != null) {
//                    newPluginMap[slug] = immutablePlugin
//                }
//            }
//            // By combining all the updated plugins into one map, we can post a single update to the UI after changes are
//            // reflected
//            updatePluginListWithNewPlugin(_featuredPlugins, newPluginMap)
//            updatePluginListWithNewPlugin(_newPlugins, newPluginMap)
//            updatePluginListWithNewPlugin(_popularPlugins, newPluginMap)
//            updatePluginListWithNewPlugin(_searchResults, newPluginMap)
//
//            // Unfortunately we can't use the same method to update the site plugins because removing/installing plugins can
//            // mess up the list. Also we care most about the Site Plugins and using the store to get the correct plugin
//            // information is much more reliable than any manual update we can make
//            site?.let {
//                _sitePlugins.updateItems(mPluginStore.getPluginDirectory(it, PluginDirectoryType.SITE))
//            }
//        }
//    }
//
//    private fun updatePluginListWithNewPlugin(pluginListNetworkResource: IListNetworkResource<ImmutablePluginModel>,
//                                              newPluginMap: Map<String, ImmutablePluginModel>) {
//        val pluginList = pluginListNetworkResource.items
//        if (pluginList.isEmpty() || newPluginMap.isEmpty()) {
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
//            pluginListNetworkResource.updateData(newList)
//        }
//    }

    fun setTitle(title: String?) {
        _title.postValue(title)
    }

    private fun getListNetworkResourceForDirectoryType(directoryType: PluginDirectoryType):
            ListNetworkResource<ImmutablePluginModel> {
        return when (directoryType) {
            PluginDirectoryType.FEATURED -> _featuredPlugins
            PluginDirectoryType.NEW -> _newPlugins
            PluginDirectoryType.POPULAR -> _popularPlugins
            PluginDirectoryType.SITE -> _sitePlugins
        }
    }

    private fun getListNetworkResourceForListType(listType: PluginListType):
            IListNetworkResource<ImmutablePluginModel> {
        return when (listType) {
            SITE -> _sitePlugins
            FEATURED -> _featuredPlugins
            POPULAR -> _popularPlugins
            NEW -> _newPlugins
            SEARCH -> _searchResults
        }
    }
}
