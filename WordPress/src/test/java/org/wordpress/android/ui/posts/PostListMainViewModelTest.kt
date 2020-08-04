package org.wordpress.android.ui.posts

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostListViewLayoutType.COMPACT
import org.wordpress.android.ui.posts.PostListViewLayoutType.STANDARD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.uploads.UploadStarter
import org.wordpress.android.util.config.WPStoriesFeatureConfig
import org.wordpress.android.viewmodel.Event

class PostListMainViewModelTest : BaseUnitTest() {
    lateinit var site: SiteModel
    private val currentBottomSheetPostId = LocalId(0)
    @Mock lateinit var uploadStarter: UploadStarter
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var savePostToDbUseCase: SavePostToDbUseCase
    @Mock lateinit var wpStoriesFeatureConfig: WPStoriesFeatureConfig
    private lateinit var viewModel: PostListMainViewModel

    @InternalCoroutinesApi
    @UseExperimental(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        val prefs = mock<AppPrefsWrapper> {
            on { postListViewLayoutType } doReturn STANDARD
        }

        site = SiteModel()

        whenever(editPostRepository.postChanged).thenReturn(MutableLiveData(Event(PostModel())))

        viewModel = PostListMainViewModel(
                dispatcher = dispatcher,
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
                uploadActionUseCase = mock(),
                savePostToDbUseCase = savePostToDbUseCase,
                wpStoriesFeatureConfig = wpStoriesFeatureConfig
        )
    }

    @Test
    fun `when started, it uploads all local drafts`() {
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())

        verify(uploadStarter, times(1)).queueUploadFromSite(eq(site))
    }

    @Test
    fun `calling onSearch() updates search query`() {
        val testSearch = "keyword"
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())

        var searchQuery: String? = null
        viewModel.searchQuery.observeForever {
            searchQuery = it
        }

        viewModel.onSearch(testSearch)

        assertThat(searchQuery).isEqualTo(testSearch)
    }

    @Test
    fun `expanding and collapsing search triggers isSearchExpanded`() {
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())

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

        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())

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
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())
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

    @Test
    fun `if currentBottomSheetPostId isn't 0 then set the post in editPostRepository from the postStore`() {
        // arrange
        val bottomSheetPostId = LocalId(2)

        // act
        viewModel.start(site, PostListRemotePreviewState.NONE, bottomSheetPostId, editPostRepository, mock())

        // assert
        verify(editPostRepository, times(1)).loadPostByLocalPostId(any())
    }

    @Test
    fun `if currentBottomSheetPostId is 0 then don't set the post in editPostRepository from the postStore`() {
        // arrange
        val bottomSheetPostId = LocalId(0)

        // act
        viewModel.start(site, PostListRemotePreviewState.NONE, bottomSheetPostId, editPostRepository, mock())

        // assert
        verify(editPostRepository, times(0)).loadPostByLocalPostId(any())
    }

    @InternalCoroutinesApi
    @Test
    fun `if post in EditPostRepository is modified then the savePostToDbUseCase should update the post`() {
        // arrange
        val editPostRepository = EditPostRepository(mock(), mock(), mock(), TEST_DISPATCHER, TEST_DISPATCHER)
        editPostRepository.set { mock() }
        val action = { _: PostModel -> true }

        // act
        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())
        // simulates the Publish Date, Status & Visibility or Tags being updated in the bottom sheet.
        editPostRepository.updateAsync(action, null)

        // assert
        verify(savePostToDbUseCase, times(1)).savePostToDb(any(), any())
    }

    @Test
    fun `if wpStoriesFeatureConfig is true and onFabClicked then _onFabClicked is called`() {
        whenever(wpStoriesFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())
        viewModel.fabClicked()

        assertThat(viewModel.onFabClicked.value?.peekContent()).isNotNull
    }

    @Test
    fun `if wpStoriesFeatureConfig is false and onFabClicked then _onFabClicked is not called`() {
        whenever(wpStoriesFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())
        viewModel.fabClicked()

        assertThat(viewModel.onFabClicked.value?.peekContent()).isNull()
    }

    @Test
    fun `if wpStoriesFeatureConfig is true and onFabLongPressed then onFabLongPressedForCreateMenu is called`() {
        whenever(wpStoriesFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())
        viewModel.onFabLongPressed()

        assertThat(viewModel.onFabLongPressedForCreateMenu.value?.peekContent()).isNotNull
    }

    @Test
    fun `if wpStoriesFeatureConfig is false and onFabLongPressed then onFabLongPressedForPostList is called`() {
        whenever(wpStoriesFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.start(site, PostListRemotePreviewState.NONE, currentBottomSheetPostId, editPostRepository, mock())
        viewModel.onFabLongPressed()

        assertThat(viewModel.onFabLongPressedForPostList.value?.peekContent()).isNotNull
    }
}
