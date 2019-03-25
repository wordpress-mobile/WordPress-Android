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
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.R.string
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
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import org.wordpress.android.util.image.ImageType.MULTI_USER_AVATAR_GREY_BACKGROUND
import org.wordpress.android.util.map
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
    private val prefs: AppPrefsWrapper,
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

    private val _isAuthorFilterSelectionVisible = MutableLiveData<Boolean>()
    val isAuthorFilterSelectionVisible: LiveData<Boolean> = _isAuthorFilterSelectionVisible

    private val _authorFilterSelection = MutableLiveData<AuthorFilterSelection>()
    val authorFilterSelection: LiveData<AuthorFilterSelection> = _authorFilterSelection

    val filterAuthorListItems: LiveData<List<AuthorFilterListItemUIState>> = _authorFilterSelection.map { selection ->
        AuthorFilterSelection.values().map mapEnumValues@{ value ->
            @ColorRes val backgroundColorRes: Int =
                    if (selection == value) R.color.grey_lighten_30_translucent_50
                    else R.color.transparent

            return@mapEnumValues when (value) {
                ME -> AuthorFilterListItemUIState.Me(accountStore.account?.avatarUrl, backgroundColorRes)
                EVERYONE -> AuthorFilterListItemUIState.Everyone(backgroundColorRes)
            }
        }
    }

    private val _selectTab = SingleLiveEvent<Int>()
    val selectTab = _selectTab as LiveData<Int>

    private val _scrollToLocalPostId = SingleLiveEvent<Int>()
    val scrollToLocalPostId = _scrollToLocalPostId as LiveData<Int>

    private val _snackBarMessage = SingleLiveEvent<SnackbarMessageHolder>()
    val snackBarMessage = _snackBarMessage as LiveData<SnackbarMessageHolder>

    fun start(site: SiteModel) {
        this.site = site

        _isAuthorFilterSelectionVisible.value = site.isWPCom
        _authorFilterSelection.value = prefs.postListAuthorSelection
    }

    override fun onCleared() {
        scrollToTargetPostJob.cancel() // cancels all coroutines with the default coroutineContext
        super.onCleared()
    }

    fun newPost() {
        _postListAction.postValue(PostListAction.NewPost(site))
    }

    fun updateAuthorFilterSelection(position: Int) {
        val selection: AuthorFilterSelection = when (position) {
            AuthorFilterSelection.ME.ordinal -> AuthorFilterSelection.ME
            AuthorFilterSelection.EVERYONE.ordinal -> AuthorFilterSelection.EVERYONE
            else -> {
                if (BuildConfig.DEBUG) {
                    throw IllegalArgumentException("Unhandled author filter selection position")
                } else {
                    AuthorFilterSelection.EVERYONE
                }
            }
        }

        _authorFilterSelection.value = selection
        prefs.postListAuthorSelection = selection
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

    sealed class AuthorFilterListItemUIState(
        val text: UiString,
        val avatarUrl: String?,
        val imageType: ImageType,
        @ColorRes val dropDownBackground: Int
    ) {
        class Everyone(@ColorRes dropDownBackground: Int) : AuthorFilterListItemUIState(
                text = UiStringRes(R.string.post_list_author_everyone),
                avatarUrl = null,
                imageType = MULTI_USER_AVATAR_GREY_BACKGROUND,
                dropDownBackground = dropDownBackground
        )

        class Me(avatarUrl: String?, @ColorRes dropDownBackground: Int) : AuthorFilterListItemUIState(
                text = UiStringRes(R.string.post_list_author_me),
                avatarUrl = avatarUrl,
                imageType = AVATAR_WITH_BACKGROUND,
                dropDownBackground = dropDownBackground
        )
    }
}
