package org.wordpress.android.ui.posts

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.ColorRes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.AuthorFilterSelection.EVERYONE
import org.wordpress.android.ui.posts.AuthorFilterSelection.ME
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

private const val SCROLL_TO_DELAY = 50L
private val FAB_VISIBLE_POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS)
val POST_LIST_PAGES = listOf(PUBLISHED, DRAFTS, SCHEDULED, TRASHED)

class PostListMainViewModel @Inject constructor(
    private val postStore: PostStore,
    private val accountStore: AccountStore,
    private val prefs: AppPrefsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val scrollToTargetPostJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + scrollToTargetPostJob

    private lateinit var site: SiteModel

    private val _viewState = MutableLiveData<PostListMainViewState>()
    val viewState: LiveData<PostListMainViewState> = _viewState

    private val _postListAction = SingleLiveEvent<PostListAction>()
    val postListAction: LiveData<PostListAction> = _postListAction

    private val _updatePostsPager = SingleLiveEvent<AuthorFilterSelection>()
    val updatePostsPager = _updatePostsPager

    private val _selectTab = SingleLiveEvent<Int>()
    val selectTab = _selectTab as LiveData<Int>

    private val _scrollToLocalPostId = SingleLiveEvent<LocalPostId>()
    val scrollToLocalPostId = _scrollToLocalPostId as LiveData<LocalPostId>

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    fun start(site: SiteModel) {
        this.site = site

        val authorFilterSelection: AuthorFilterSelection = prefs.postListAuthorSelection

        _updatePostsPager.value = authorFilterSelection
        _viewState.value = PostListMainViewState(
                isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(POST_LIST_PAGES.first()),
                isAuthorFilterVisible = site.isUsingWpComRestApi,
                authorFilterSelection = authorFilterSelection,
                authorFilterItems = getAuthorFilterItems(authorFilterSelection)
        )
    }

    override fun onCleared() {
        scrollToTargetPostJob.cancel() // cancels all coroutines with the default coroutineContext
        super.onCleared()
    }

    fun newPost() {
        _postListAction.postValue(PostListAction.NewPost(site))
    }

    fun updateAuthorFilterSelection(selectionId: Long) {
        val selection = AuthorFilterSelection.fromId(selectionId)

        updateViewStateTriggerPagerChange(
                authorFilterSelection = selection,
                authorFilterItems = getAuthorFilterItems(selection)
        )
        prefs.postListAuthorSelection = selection
    }

    fun onTabChanged(position: Int) {
        val currentPage = POST_LIST_PAGES[position]
        updateViewStateTriggerPagerChange(isFabVisible = FAB_VISIBLE_POST_LIST_PAGES.contains(currentPage))
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
                _scrollToLocalPostId.value = LocalPostId(LocalId(postModel.id))
            }
        }
    }

    private fun getAuthorFilterItems(selection: AuthorFilterSelection): List<AuthorFilterListItemUIState> {
        return AuthorFilterSelection.values().map { value ->
            @ColorRes val backgroundColorRes: Int =
                    if (selection == value) R.color.grey_lighten_30_translucent_50
                    else R.color.transparent

            when (value) {
                ME -> AuthorFilterListItemUIState.Me(accountStore.account?.avatarUrl, backgroundColorRes)
                EVERYONE -> AuthorFilterListItemUIState.Everyone(backgroundColorRes)
            }
        }
    }

    /**
     * Only the non-null variables will be changed in the current state
     */
    private fun updateViewStateTriggerPagerChange(
        isFabVisible: Boolean? = null,
        isAuthorFilterVisible: Boolean? = null,
        authorFilterSelection: AuthorFilterSelection? = null,
        authorFilterItems: List<AuthorFilterListItemUIState>? = null
    ) {
        val currentState = requireNotNull(viewState.value) {
            "updateViewStateTriggerPagerChange should not be called before the initial state is set"
        }

        _viewState.value = PostListMainViewState(
                isFabVisible ?: currentState.isFabVisible,
                isAuthorFilterVisible ?: currentState.isAuthorFilterVisible,
                authorFilterSelection ?: currentState.authorFilterSelection,
                authorFilterItems ?: currentState.authorFilterItems
        )

        if (authorFilterSelection != null && currentState.authorFilterSelection != authorFilterSelection) {
            _updatePostsPager.value = authorFilterSelection
        }
    }
}
