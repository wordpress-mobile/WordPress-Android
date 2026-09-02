package org.wordpress.android.ui.posts.prepublishing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.FeaturedImageHelper
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageData
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageState
import org.wordpress.android.ui.posts.FeaturedImageHelper.TrackableEvent
import org.wordpress.android.ui.posts.UpdateFeaturedImageUseCase
import org.wordpress.android.ui.posts.prepublishing.featuredimage.PrepublishingFeaturedImageViewModel
import org.wordpress.android.ui.posts.prepublishing.featuredimage.PrepublishingFeaturedImageViewModel.UiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event

private const val POST_ID = 1

@ExperimentalCoroutinesApi
class PrepublishingFeaturedImageViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingFeaturedImageViewModel

    @Mock
    lateinit var featuredImageHelper: FeaturedImageHelper

    @Mock
    lateinit var updateFeaturedImageUseCase: UpdateFeaturedImageUseCase

    @Mock
    lateinit var editPostRepository: EditPostRepository

    @Mock
    lateinit var post: PostImmutableModel

    private val site: SiteModel = SiteModel()

    @Before
    fun setup() {
        viewModel = PrepublishingFeaturedImageViewModel(
            featuredImageHelper,
            updateFeaturedImageUseCase,
            testDispatcher()
        )
        whenever(editPostRepository.getPost()).thenReturn(post)
        whenever(post.id).thenReturn(POST_ID)
        stubFeaturedImageState(FeaturedImageState.IMAGE_EMPTY)
    }

    private fun stubFeaturedImageState(state: FeaturedImageState) {
        whenever(featuredImageHelper.createCurrentFeaturedImageState(site, post))
            .thenReturn(FeaturedImageData(state, null))
    }

    @Test
    fun `when started with no featured image the remove button is hidden and set button shows set text`() {
        var uiState: UiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository, site)

        assertThat(uiState?.isRemoveButtonVisible).isFalse
        assertThat((uiState?.setButtonText as UiStringRes).stringRes)
            .isEqualTo(R.string.post_settings_set_featured_image)
    }

    @Test
    fun `when started with a featured image the remove button is shown and set button shows change text`() {
        stubFeaturedImageState(FeaturedImageState.REMOTE_IMAGE_LOADING)
        var uiState: UiState? = null
        viewModel.uiState.observeForever { uiState = it }

        viewModel.start(editPostRepository, site)

        assertThat(uiState?.isRemoveButtonVisible).isTrue
        assertThat((uiState?.setButtonText as UiStringRes).stringRes)
            .isEqualTo(R.string.post_settings_choose_featured_image)
    }

    @Test
    fun `when set or change is clicked the featured image picker is launched with the post id`() {
        var event: Event<Int>? = null
        viewModel.launchFeaturedImagePicker.observeForever { event = it }

        viewModel.start(editPostRepository, site)
        viewModel.onSetOrChangeClicked()

        assertThat(event?.peekContent()).isEqualTo(POST_ID)
        verify(featuredImageHelper).trackFeaturedImageEvent(TrackableEvent.IMAGE_SET_CLICKED, POST_ID)
    }

    @Test
    fun `when remove is clicked the featured image is cleared and pending upload is cancelled`() {
        viewModel.start(editPostRepository, site)
        viewModel.onRemoveClicked()

        verify(featuredImageHelper).cancelFeaturedImageUpload(site, post, false)
        verify(updateFeaturedImageUseCase).updateFeaturedImage(eq(0L), eq(editPostRepository), any())
    }

    @Test
    fun `when set or change is clicked without a post the picker is not launched`() {
        var event: Event<Int>? = null
        viewModel.launchFeaturedImagePicker.observeForever { event = it }

        viewModel.start(editPostRepository, site)
        whenever(editPostRepository.getPost()).thenReturn(null)
        viewModel.onSetOrChangeClicked()

        assertThat(event).isNull()
        verify(featuredImageHelper, never()).trackFeaturedImageEvent(any(), any())
    }

    @Test
    fun `when back is clicked navigateToHomeScreen is triggered`() {
        var event: Event<Unit>? = null
        viewModel.navigateToHomeScreen.observeForever { event = it }

        viewModel.onBackButtonClick()

        assertThat(event).isNotNull
    }
}
