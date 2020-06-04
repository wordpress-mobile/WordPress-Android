package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostListViewLayoutType.COMPACT
import org.wordpress.android.ui.posts.PostListViewLayoutType.STANDARD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadStarter

class PostListMainViewModelTest : BaseUnitTest() {
    lateinit var site: SiteModel
    private val currentBottomSheetPostId = LocalId(0)
    @Mock lateinit var uploadStarter: UploadStarter
    private lateinit var viewModel: PostListMainViewModel

    @UseExperimental(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        val prefs = mock<AppPrefsWrapper> {
            on { postListViewLayoutType } doReturn STANDARD
        }

        site = SiteModel()

        viewModel = PostListMainViewModel(
                dispatcher = mock(),
                postStore = mock(),
                accountStore = mock(),
                uploadStore = mock(),
                mediaStore = mock(),
                networkUtilsWrapper = mock(),
                prefs = prefs,
                previewStateHelper = mock(),
                analyticsTracker = mock(),
                mainDispatcher = Dispatchers.Unconfined,
                bgDispatcher = Dispatchers.Unconfined,
                postListEventListenerFactory = mock(),
                uploadStarter = uploadStarter,
                uploadActionUseCase = mock()
        )
    }

    @Test
    fun `when started, it uploads all local drafts`() {
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())

        verify(uploadStarter, times(1)).queueUploadFromSite(eq(site))
    }

    @Test
    fun `search is available for wpcom and jetpack sites`() {
        site.origin = SiteModel.ORIGIN_WPCOM_REST
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())

        var isSearchAvailable = false
        viewModel.isSearchAvailable.observeForever {
            isSearchAvailable = it
        }

        assertThat(isSearchAvailable).isTrue()
    }

    @Test
    fun `search is not available for xmlrpc sites`() {
        site.origin = SiteModel.ORIGIN_XMLRPC
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())

        var isSearchAvailable = true
        viewModel.isSearchAvailable.observeForever {
            isSearchAvailable = it
        }

        assertThat(isSearchAvailable).isFalse()
    }

    @Test
    fun `calling onSearch() updates search query`() {
        val testSearch = "keyword"
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())

        var searchQuery: String? = null
        viewModel.searchQuery.observeForever {
            searchQuery = it
        }

        viewModel.onSearch(testSearch)

        assertThat(searchQuery).isEqualTo(testSearch)
    }

    @Test
    fun `expanding and collapsing search triggers isSearchExpanded`() {
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())

        var isSearchExpanded = false
        viewModel.isSearchExpanded.observeForever {
            isSearchExpanded = it
        }

        viewModel.onSearchExpanded(false)
        assertThat(isSearchExpanded).isTrue()

        viewModel.onSearchCollapsed(delay = 0)
        assertThat(isSearchExpanded).isFalse()
    }

    @Test
    fun `expanding search after configuration change preserves search query`() {
        val testSearch = "keyword"

        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())

        var searchQuery: String? = null
        viewModel.searchQuery.observeForever {
            searchQuery = it
        }

        viewModel.onSearch(testSearch)

        assertThat(searchQuery).isNotNull()
        assertThat(searchQuery).isEqualTo(testSearch)

        viewModel.onSearchExpanded(true)
        assertThat(searchQuery).isEqualTo(testSearch)

        viewModel.onSearchCollapsed(0)

        viewModel.onSearchExpanded(false)
        assertThat(searchQuery).isNull()
    }

    @Test
    fun `search is using compact view mode independently from normal post list`() {
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, mock())
        assertThat(viewModel.viewLayoutType.value).isEqualTo(STANDARD) // default value

        var viewLayoutType: PostListViewLayoutType? = null
        viewModel.viewLayoutType.observeForever {
            viewLayoutType = it
        }

        viewModel.onSearchExpanded(false)

        assertThat(viewLayoutType).isEqualTo(COMPACT)

        viewModel.onSearchCollapsed()

        assertThat(viewLayoutType).isEqualTo(STANDARD)
    }
}
