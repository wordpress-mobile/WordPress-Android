package org.wordpress.android.ui.mysite

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_UPLOADED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.MY_SITE_ICON_UPLOAD_UNSUCCESSFUL
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel.MediaUploaded
import org.wordpress.android.ui.mysite.SiteIconUploadHandler.ItemUploadedModel.PostUploaded
import org.wordpress.android.ui.uploads.UploadService.UploadErrorEvent
import org.wordpress.android.ui.uploads.UploadService.UploadMediaSuccessEvent
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
class SiteIconUploadHandlerTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var eventBusWrapper: EventBusWrapper

    @Mock
    lateinit var postModel: PostModel

    @Mock
    lateinit var mediaModel: MediaModel

    @Mock
    lateinit var siteModel: SiteModel
    private var uploadedEvents = mutableListOf<ItemUploadedModel>()
    private lateinit var viewModel: SiteIconUploadHandler

    @Before
    fun setUp() {
        viewModel = SiteIconUploadHandler(selectedSiteRepository, analyticsTrackerWrapper, eventBusWrapper)
        uploadedEvents = mutableListOf()
        viewModel.onUploadedItem.observeForever {
            it?.getContentIfNotHandled()?.let { model ->
                uploadedEvents.add(model)
            }
        }
    }

    @Test
    fun `view model gets registered on creation`() {
        verify(eventBusWrapper).register(viewModel)
    }

    @Test
    fun `on post upload error hides progress bar, removes event, propagates event if site present and matches ID`() {
        val message = "error message"
        val event = UploadErrorEvent(postModel, message)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        val siteLocalId = 1
        whenever(siteModel.id).thenReturn(siteLocalId)
        whenever(postModel.localSiteId).thenReturn(siteLocalId)

        viewModel.onEventMainThread(event)

        verify(analyticsTrackerWrapper).track(MY_SITE_ICON_UPLOAD_UNSUCCESSFUL)
        verify(eventBusWrapper).removeStickyEvent(event)
        verify(selectedSiteRepository).showSiteIconProgressBar(false)
        assertThat(uploadedEvents).containsOnly(PostUploaded(postModel, siteModel, message))
    }

    @Test
    fun `on post upload error does not propagate event if site not present`() {
        val message = "error message"
        val event = UploadErrorEvent(postModel, message)

        viewModel.onEventMainThread(event)

        assertThat(uploadedEvents).isEmpty()
    }

    @Test
    fun `on post upload error does not propagate event if site ID does not match post site ID`() {
        val message = "error message"
        val event = UploadErrorEvent(postModel, message)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(siteModel.id).thenReturn(1)
        whenever(postModel.localSiteId).thenReturn(2)

        viewModel.onEventMainThread(event)

        assertThat(uploadedEvents).isEmpty()
    }

    @Test
    fun `on media upload error hides progress bar, removes event, propagates event if media not empty`() {
        val message = "error message"
        val event = UploadErrorEvent(listOf(mediaModel), message)

        viewModel.onEventMainThread(event)

        verify(analyticsTrackerWrapper).track(MY_SITE_ICON_UPLOAD_UNSUCCESSFUL)
        verify(eventBusWrapper).removeStickyEvent(event)
        verify(selectedSiteRepository).showSiteIconProgressBar(false)
        assertThat(uploadedEvents).containsOnly(MediaUploaded(listOf(mediaModel), null, message))
    }

    @Test
    fun `on media upload error hides progress bar, removes event, does not propagate event if media empty`() {
        val message = "error message"
        val event = UploadErrorEvent(listOf(), message)

        viewModel.onEventMainThread(event)

        verify(analyticsTrackerWrapper).track(MY_SITE_ICON_UPLOAD_UNSUCCESSFUL)
        verify(eventBusWrapper).removeStickyEvent(event)
        verify(selectedSiteRepository).showSiteIconProgressBar(false)
        assertThat(uploadedEvents).isEmpty()
    }

    @Test
    fun `on media upload success removes event, updates icon ID if media not empty and icon upload in progress`() {
        val message = "success message"
        val event = UploadMediaSuccessEvent(listOf(mediaModel), message)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(selectedSiteRepository.isSiteIconUploadInProgress()).thenReturn(true)
        val mediaId: Long = 10
        whenever(mediaModel.mediaId).thenReturn(mediaId)

        viewModel.onEventMainThread(event)

        verify(analyticsTrackerWrapper).track(MY_SITE_ICON_UPLOADED)
        verify(eventBusWrapper).removeStickyEvent(event)
        verify(selectedSiteRepository).updateSiteIconMediaId(mediaId.toInt(), true)
    }

    @Test
    fun `on media upload success removes event, propagates event if media not empty and icon upload not in progress`() {
        val message = "success message"
        val event = UploadMediaSuccessEvent(listOf(mediaModel), message)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(selectedSiteRepository.isSiteIconUploadInProgress()).thenReturn(false)

        viewModel.onEventMainThread(event)

        verify(analyticsTrackerWrapper).track(MY_SITE_ICON_UPLOADED)
        verify(eventBusWrapper).removeStickyEvent(event)
        assertThat(uploadedEvents).containsOnly(MediaUploaded(listOf(mediaModel), siteModel, message))
    }
}
