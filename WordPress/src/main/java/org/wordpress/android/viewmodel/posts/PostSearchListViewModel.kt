package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import javax.inject.Named

class PostSearchListViewModel
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    @Named(UI_SCOPE) private val uiScope: CoroutineScope
) : ViewModel() {
    private val _searchResult: MutableLiveData<PagedPostList> = MutableLiveData()
    val searchResult: LiveData<PagedPostList> = _searchResult

    private var isStarted: Boolean = false
    private lateinit var postsViewModel: PostListViewModel

    fun start(postsViewModel: PostListViewModel) {
        this.postsViewModel = postsViewModel

        if (!isStarted) {
            isStarted = true


            postsViewModel.searchListData.observeForever(searchObserver)
        }
    }

    override fun onCleared() {
        postsViewModel.searchListData.removeObserver(searchObserver)
    }

    private val searchObserver = Observer<PagedPostList> { posts ->
        if (posts != null) {
            _searchResult.value = posts
        }
//        else {
//             _searchResult.value = listOf(Empty(string.pages_empty_search_result, true))
//        }
    }

//    fun onMenuAction(action: Action, pageItem: Page): Boolean {
//        return pagesViewModel.onMenuAction(action, pageItem)
//    }
//
//    fun onItemTapped(pageItem: Page) {
//        pagesViewModel.onItemTapped(pageItem)
//    }

//    private fun loadFoundPages(pages: SortedMap<PageListType, List<PageModel>>) = uiScope.launch {
//        if (pages.isNotEmpty()) {
//            val pageItems = pages
//                    .map { (listType, results) ->
//                        listOf(Divider(resourceProvider.getString(listType.title))) +
//                                results.map { it.toPageItem(pagesViewModel.arePageActionsEnabled) }
//                    }
//                    .fold(mutableListOf()) { acc: MutableList<PageItem>, list: List<PageItem> ->
//                        acc.addAll(list)
//                        return@fold acc
//                    }
//            _searchResult.value = pageItems
//        } else {
//            _searchResult.value = listOf(Empty(string.pages_empty_search_result, true))
//        }
//    }
//
//    private fun PageModel.toPageItem(areActionsEnabled: Boolean): PageItem {
//        return when (status) {
//            PageStatus.PUBLISHED, PageStatus.PRIVATE ->
//                PublishedPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
//            PageStatus.DRAFT, PageStatus.PENDING -> DraftPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
//            PageStatus.TRASHED -> TrashedPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
//            PageStatus.SCHEDULED -> ScheduledPage(remoteId, title, date, actionsEnabled = areActionsEnabled)
//        }
//    }
}
