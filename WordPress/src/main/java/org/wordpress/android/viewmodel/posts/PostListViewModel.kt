package org.wordpress.android.viewmodel.posts

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import de.greenrobot.event.EventBus
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListItemsChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import javax.inject.Inject

class PostListViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val listStore: ListStore,
    private val postStore: PostStore
) : ViewModel() {
    private var isStarted: Boolean = false
    private val listManagerMutableLiveData = MutableLiveData<ListManager<PostModel>>()
    private var listDescriptor: PostListDescriptor? = null
    private var site: SiteModel? = null

    init {
        EventBus.getDefault().register(this)
        dispatcher.register(this)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun start(site: SiteModel, listDescriptor: PostListDescriptor) {
        if (isStarted) {
            return
        }
        this.site = site
        this.listDescriptor = listDescriptor
        refreshListManagerFromStore(refreshFirstPageAfter = true)
        isStarted = true
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        listDescriptor?.let {
            if (!event.listDescriptors.contains(it)) {
                return
            }
            refreshListManagerFromStore()
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListItemsChanged(event: OnListItemsChanged) {
        if (listDescriptor?.typeIdentifier != event.type) {
            return
        }
        refreshListManagerFromStore()
    }

    private fun refreshListManagerFromStore(refreshFirstPageAfter: Boolean = false) {
        listDescriptor?.let {
            GlobalScope.launch(Dispatchers.Default) {
                val listManager = getListManagerFromStore(it)
                listManagerMutableLiveData.postValue(listManager)
                if (refreshFirstPageAfter) {
                    listManager.refresh()
                }
            }
        }
    }

    private suspend fun getListManagerFromStore(listDescriptor: PostListDescriptor) = withContext(Dispatchers.Default) {
        listStore.getListManager(listDescriptor, object : ListItemDataSource<PostModel> {
            /**
             * Tells [ListStore] how to fetch a post from remote for the given list descriptor and remote post id
             */
            override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
                site?.let {
                    val postToFetch = PostModel()
                    postToFetch.remotePostId = remoteItemId
                    val payload = RemotePostPayload(postToFetch, it)
                    dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
                }
            }

            /**
             * Tells [ListStore] how to fetch a list from remote for the given list descriptor and offset
             */
            override fun fetchList(listDescriptor: ListDescriptor, offset: Int) {
                if (listDescriptor is PostListDescriptor) {
                    val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
                    dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
                }
            }

            /**
             * Tells [ListStore] how to get posts from [PostStore] for the given list descriptor and remote post ids
             */
            override fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, PostModel> {
                site?.let {
                    return postStore.getPostsByRemotePostIds(remoteItemIds, it)
                }
                return emptyMap()
            }

            /**
             * Tells [ListStore] which local drafts should be included in the list. Since [ListStore] deals with
             * remote items, it needs our help to show local data.
             */
            override fun localItems(listDescriptor: ListDescriptor): List<PostModel>? {
                // TODO!!
//                if (listDescriptor is PostListDescriptor) {
//                    // We should filter out the trashed posts from local drafts since they should be hidden
//                    val trashedLocalPostIds = trashedPostIds.map { it.first }
//                    return postStore.getLocalPostsForDescriptor(listDescriptor)
//                            .filter { !trashedLocalPostIds.contains(it.id) }
//                }
                return null
            }

            /**
             * Tells [ListStore] which remote post ids must be included in the list. This is to workaround a case
             * where the local draft is uploaded to remote but the list has not been refreshed yet. If we don't
             * tell about this to [ListStore] that post will disappear until the next refresh.
             *
             * Please check out [OnPostUploaded] and [OnListChanged] for where [uploadedPostRemoteIds] is managed.
             */
            override fun remoteItemIdsToInclude(listDescriptor: ListDescriptor): List<Long>? {
                // TODO!!
                return null
//                return uploadedPostRemoteIds
            }

            /**
             * Tells [ListStore] which remote post ids must be hidden from the list. In order to show an undo
             * snackbar when a post is trashed, we don't immediately delete/trash a post which means [ListStore]
             * doesn't know about this action and needs our help to determine which posts should be hidden until
             * delete/trash action is completed.
             *
             * Please check out [trashPost] for more details.
             */
            override fun remoteItemsToHide(listDescriptor: ListDescriptor): List<Long>? {
                // TODO!!
                return null
//                return trashedPostIds.map { it.second }
            }
        })
    }
}
