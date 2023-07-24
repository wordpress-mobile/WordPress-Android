package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.PostUIModel
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BlazeViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var mediaStore: MediaStore

    @Mock
    private lateinit var dispatcher: Dispatcher

    private lateinit var blazeViewModel: BlazeViewModel

    @Before
    fun setUp() {
        blazeViewModel = BlazeViewModel(
            blazeFeatureUtils,
            dispatcher,
            mediaStore,
            selectedSiteRepository
        )
    }

    @Test
    fun `given getSelectedSite is null, when xx, then exception`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val model = PostUIModel(postId = 1L, title = "title", featuredImageId = 1L,
            url = "url", featuredImageUrl = "featuredImageUrl"
        )
        val result = blazeViewModel.start(BlazeFlowSource.POSTS_LIST, model)


        Assertions.assertThat(result).isNotNull
    }
}
