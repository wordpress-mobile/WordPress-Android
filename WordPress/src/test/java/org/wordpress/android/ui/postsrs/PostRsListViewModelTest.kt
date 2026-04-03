package org.wordpress.android.ui.postsrs

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.WpServiceProvider
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class PostRsListViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock lateinit var serviceProvider: WpServiceProvider
    @Mock lateinit var restClient: PostRsRestClient
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var postStore: PostStore
    @Mock lateinit var blazeFeatureUtils: BlazeFeatureUtils
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock lateinit var accountStore: AccountStore
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var site: SiteModel
    private var activeViewModel: PostRsListViewModel? = null

    @Before
    fun setUp() {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(appPrefsWrapper.postListAuthorSelection)
            .thenReturn(AuthorFilterSelection.EVERYONE)
        whenever(resourceProvider.getString(any())).thenReturn("error")
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel() = PostRsListViewModel(
        selectedSiteRepository = selectedSiteRepository,
        serviceProvider = serviceProvider,
        restClient = restClient,
        resourceProvider = resourceProvider,
        postStore = postStore,
        blazeFeatureUtils = blazeFeatureUtils,
        networkUtilsWrapper = networkUtilsWrapper,
        accountStore = accountStore,
        appPrefsWrapper = appPrefsWrapper,
        analyticsTracker = analyticsTracker,
    ).also { activeViewModel = it }

    @Test
    fun `when no site selected, emits ShowToast and Finish`() = test {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(null)

        val viewModel = createViewModel()

        viewModel.events.test {
            val first = awaitItem()
            assertThat(first)
                .isInstanceOf(PostRsListEvent.ShowToast::class.java)
            assertThat((first as PostRsListEvent.ShowToast).messageResId)
                .isEqualTo(R.string.blog_not_found)

            val second = awaitItem()
            assertThat(second).isEqualTo(PostRsListEvent.Finish)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tab states are empty on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `search is inactive on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.isSearchActive.value).isFalse()
    }

    @Test
    fun `search query is empty on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.searchQuery.value).isEmpty()
    }

    @Test
    fun `pending confirmation is null on init`() {
        val viewModel = createViewModel()
        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `onSearchOpen sets isSearchActive to true`() {
        val viewModel = createViewModel()

        viewModel.onSearchOpen()

        assertThat(viewModel.isSearchActive.value).isTrue()
    }

    @Test
    fun `onSearchClose sets isSearchActive to false`() {
        val viewModel = createViewModel()
        viewModel.onSearchOpen()

        viewModel.onSearchClose(PostRsListTab.PUBLISHED)

        assertThat(viewModel.isSearchActive.value).isFalse()
    }

    @Test
    fun `onSearchClose clears search query`() {
        val viewModel = createViewModel()
        viewModel.onSearchOpen()
        viewModel.onSearchQueryChanged(
            "test query", PostRsListTab.PUBLISHED
        )

        viewModel.onSearchClose(PostRsListTab.PUBLISHED)

        assertThat(viewModel.searchQuery.value).isEmpty()
    }

    @Test
    fun `onSearchQueryChanged updates search query`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChanged(
            "hello", PostRsListTab.PUBLISHED
        )

        assertThat(viewModel.searchQuery.value).isEqualTo("hello")
    }

    @Test
    fun `onSearchQueryChanged with blank clears tab states`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChanged("", PostRsListTab.PUBLISHED)

        assertThat(viewModel.tabStates.value).isEmpty()
    }

    @Test
    fun `onAuthorFilterChanged updates author filter`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(
            AuthorFilterSelection.ME, PostRsListTab.PUBLISHED
        )

        assertThat(viewModel.authorFilter.value)
            .isEqualTo(AuthorFilterSelection.ME)
    }

    @Test
    fun `onAuthorFilterChanged persists preference`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(
            AuthorFilterSelection.ME, PostRsListTab.PUBLISHED
        )

        verify(appPrefsWrapper).postListAuthorSelection =
            AuthorFilterSelection.ME
    }

    @Test
    fun `onAuthorFilterChanged ignores same selection`() {
        val viewModel = createViewModel()

        viewModel.onAuthorFilterChanged(
            AuthorFilterSelection.EVERYONE, PostRsListTab.PUBLISHED
        )

        verify(appPrefsWrapper, never()).postListAuthorSelection =
            any()
    }

    @Test
    fun `openPost on trashed tab sets MoveToDraft confirmation`() {
        val viewModel = createViewModel()

        viewModel.openPost(42L, PostRsListTab.TRASHED)

        assertThat(viewModel.pendingConfirmation.value)
            .isEqualTo(PendingConfirmation.MoveToDraft(42L))
    }

    @Test
    fun `openPost emits EditPost when FluxC post found`() = test {
        val postModel = PostModel()
        whenever(postStore.getPostByRemotePostId(42L, site))
            .thenReturn(postModel)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.openPost(42L, PostRsListTab.PUBLISHED)

            val event = awaitItem()
            assertThat(event)
                .isInstanceOf(PostRsListEvent.EditPost::class.java)
            val editEvent = event as PostRsListEvent.EditPost
            assertThat(editEvent.post).isEqualTo(postModel)
            assertThat(editEvent.site).isEqualTo(site)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openPost shows snackbar when FluxC post not found`() = test {
        whenever(postStore.getPostByRemotePostId(42L, site))
            .thenReturn(null)
        val viewModel = createViewModel()

        viewModel.snackbarMessages.test {
            viewModel.openPost(42L, PostRsListTab.PUBLISHED)

            val message = awaitItem()
            assertThat(message.message).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createNewPost emits CreatePost event`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.createNewPost()

            val event = awaitItem()
            assertThat(event)
                .isInstanceOf(PostRsListEvent.CreatePost::class.java)
            assertThat((event as PostRsListEvent.CreatePost).site)
                .isEqualTo(site)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPostMenuAction TRASH sets Trash confirmation`() {
        val viewModel = createViewModel()

        viewModel.onPostMenuAction(42L, PostRsMenuAction.TRASH)

        assertThat(viewModel.pendingConfirmation.value)
            .isEqualTo(PendingConfirmation.Trash(42L))
    }

    @Test
    fun `onPostMenuAction DELETE sets Delete confirmation`() {
        val viewModel = createViewModel()

        viewModel.onPostMenuAction(
            42L, PostRsMenuAction.DELETE_PERMANENTLY
        )

        assertThat(viewModel.pendingConfirmation.value)
            .isEqualTo(PendingConfirmation.Delete(42L))
    }

    @Test
    fun `onPostMenuAction READ emits ReadPost event`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onPostMenuAction(42L, PostRsMenuAction.READ)

            val event = awaitItem()
            assertThat(event)
                .isInstanceOf(PostRsListEvent.ReadPost::class.java)
            val readEvent = event as PostRsListEvent.ReadPost
            assertThat(readEvent.blogId).isEqualTo(site.siteId)
            assertThat(readEvent.postId).isEqualTo(42L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPostMenuAction COMMENTS emits ViewComments`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onPostMenuAction(
                42L, PostRsMenuAction.COMMENTS
            )

            val event = awaitItem()
            assertThat(event).isInstanceOf(
                PostRsListEvent.ViewComments::class.java
            )
            val commentsEvent =
                event as PostRsListEvent.ViewComments
            assertThat(commentsEvent.blogId).isEqualTo(site.siteId)
            assertThat(commentsEvent.postId).isEqualTo(42L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPostMenuAction STATS emits ViewStats`() = test {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onPostMenuAction(42L, PostRsMenuAction.STATS)

            val event = awaitItem()
            assertThat(event).isInstanceOf(
                PostRsListEvent.ViewStats::class.java
            )
            val statsEvent = event as PostRsListEvent.ViewStats
            assertThat(statsEvent.site).isEqualTo(site)
            assertThat(statsEvent.postId).isEqualTo(42L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPostMenuAction VIEW does not crash when post unknown`() {
        val viewModel = createViewModel()

        viewModel.onPostMenuAction(42L, PostRsMenuAction.VIEW)

        // No event emitted and no crash — post link is null
        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `onDismissPendingAction clears pending confirmation`() {
        val viewModel = createViewModel()
        viewModel.onPostMenuAction(42L, PostRsMenuAction.TRASH)
        assertThat(viewModel.pendingConfirmation.value).isNotNull()

        viewModel.onDismissPendingAction()

        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `onConfirmPendingAction with null pending does nothing`() {
        val viewModel = createViewModel()
        assertThat(viewModel.pendingConfirmation.value).isNull()

        viewModel.onConfirmPendingAction()

        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `onConfirmPendingAction clears pending confirmation`() {
        val viewModel = createViewModel()
        viewModel.onPostMenuAction(42L, PostRsMenuAction.TRASH)

        viewModel.onConfirmPendingAction()

        assertThat(viewModel.pendingConfirmation.value).isNull()
    }

    @Test
    fun `isAuthorFilterSupported is false for non-WPCom sites`() {
        site.setIsWPCom(false)
        val viewModel = createViewModel()

        assertThat(viewModel.isAuthorFilterSupported).isFalse()
    }

    @Test
    fun `isAuthorFilterSupported is false for single user sites`() {
        site.setIsWPCom(true)
        site.hasCapabilityEditOthersPosts = true
        site.setIsSingleUserSite(true)
        val viewModel = createViewModel()

        assertThat(viewModel.isAuthorFilterSupported).isFalse()
    }

    @Test
    fun `isAuthorFilterSupported is false without edit others`() {
        site.setIsWPCom(true)
        site.hasCapabilityEditOthersPosts = false
        site.setIsSingleUserSite(false)
        val viewModel = createViewModel()

        assertThat(viewModel.isAuthorFilterSupported).isFalse()
    }

    @Test
    fun `isAuthorFilterSupported is true when all conditions met`() {
        site.setIsWPCom(true)
        site.hasCapabilityEditOthersPosts = true
        site.setIsSingleUserSite(false)
        val viewModel = createViewModel()

        assertThat(viewModel.isAuthorFilterSupported).isTrue()
    }

    @Test
    fun `onConfirmPendingAction shows snackbar when offline`() =
        test {
            whenever(networkUtilsWrapper.isNetworkAvailable())
                .thenReturn(false)
            val viewModel = createViewModel()
            viewModel.onPostMenuAction(42L, PostRsMenuAction.TRASH)

            viewModel.snackbarMessages.test {
                viewModel.onConfirmPendingAction()

                val message = awaitItem()
                assertThat(message.message).isNotEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
