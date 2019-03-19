package org.wordpress.android.ui.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val SCROLL_TO_DELAY = 50L
private val FAB_VISIBLE_POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS)
val POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)

class PostListMainViewModel @Inject constructor(
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val scrollToTargetPostJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + scrollToTargetPostJob

    private lateinit var site: SiteModel

    private val _postListAction = SingleLiveEvent<PostListAction>()
    val postListAction: LiveData<PostListAction> = _postListAction

    private val _isFabVisible = MutableLiveData<Boolean>()
    val isFabVisible: LiveData<Boolean> = _isFabVisible

    private val _avatarUrl = MutableLiveData<String>()
    val avatarUrl: LiveData<String> = _avatarUrl

    private val _filterOnlyUser = MutableLiveData<Boolean>()
    val filterOnlyUser: LiveData<Boolean> = _filterOnlyUser

    private val _selectTab = SingleLiveEvent<Int>()
    val selectTab = _selectTab as LiveData<Int>

    private val _scrollToLocalPostId = SingleLiveEvent<Int>()
    val scrollToLocalPostId = _scrollToLocalPostId as LiveData<Int>

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    fun start(site: SiteModel) {
        this.site = site
        _avatarUrl.value = accountStore?.account?.avatarUrl ?: ""
    }

    override fun onCleared() {
        scrollToTargetPostJob.cancel() // cancels all coroutines with the default coroutineContext
        super.onCleared()
    }

    fun newPost() {
        _postListAction.postValue(PostListAction.NewPost(site))
    }

    fun onAuthorSelectionChanged(onlyUser: Boolean) {
        _filterOnlyUser.value = onlyUser
    }

    fun onTabChanged(position: Int) {
        val currentPage = POST_LIST_PAGES[position]
        _isFabVisible.value = FAB_VISIBLE_POST_LIST_PAGES.contains(currentPage)
    }

    fun showTargetPost(targetPostId: Int) {
        val postModel = postStore.getPostByLocalPostId(targetPostId)
        if (postModel == null) {
            _snackBarMessage.value = SnackbarMessageHolder(string.error_post_does_not_exist)
        } else {
            launch(mainDispatcher) {
                val targetTab = PostListType.fromPostStatus(PostStatus.fromPost(postModel))
                _selectTab.value = POST_LIST_PAGES.indexOf(targetTab)
                // we need to make sure the ViewPager initializes the targetTab fragment before we can propagate
                // the targetPostId to it
                withContext(bgDispatcher) {
                    delay(SCROLL_TO_DELAY)
                }
                _scrollToLocalPostId.value = postModel.id
            }
        }
    }
}
