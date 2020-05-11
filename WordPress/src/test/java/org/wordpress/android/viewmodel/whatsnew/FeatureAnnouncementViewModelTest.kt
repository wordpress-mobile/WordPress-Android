package org.wordpress.android.viewmodel.whatsnew

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel.FeatureAnnouncementUiModel
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class FeatureAnnouncementViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: FeatureAnnouncementViewModel
    @Mock lateinit var onDialogClosedObserver: Observer<Unit>
    @Mock lateinit var onAnnouncementDetailsRequestedObserver: Observer<String>
    @Mock lateinit var featuresObserver: Observer<List<FeatureAnnouncementItem>>
    @Mock lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private val uiModelResults = mutableListOf<FeatureAnnouncementUiModel>()

    private val testFeatures = listOf(
            FeatureAnnouncementItem(
                    "Test Feature 1",
                    "Test Description 1",
                    "https://wordpress.org/icon1.png"
            ),
            FeatureAnnouncementItem(
                    "Test Feature 2",
                    "Test Description 1",
                    "https://wordpress.org/icon2.png"
            ),
            FeatureAnnouncementItem(
                    "Test Feature 3",
                    "Test Description 3",
                    "https://wordpress.org/icon3.png"
            )
    )

    private val featureAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "https://wordpress.org/",
            testFeatures
    )

    @Before
    fun setUp() {
        uiModelResults.clear()

        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement()).thenReturn(featureAnnouncement)

        viewModel = FeatureAnnouncementViewModel(
                featureAnnouncementProvider,
                analyticsTrackerWrapper,
                NoDelayCoroutineDispatcher()
        )
        viewModel.uiModel.observeForever { if (it != null) uiModelResults.add(it) }
        viewModel.onDialogClosed.observeForever(onDialogClosedObserver)
        viewModel.onAnnouncementDetailsRequested.observeForever(onAnnouncementDetailsRequestedObserver)
        viewModel.featureItems.observeForever(featuresObserver)

        viewModel.start()
    }

    @Test
    fun `progress is visible after start`() {
        assertThat(uiModelResults.isNotEmpty()).isEqualTo(true)
        assertThat(uiModelResults[0].isProgressVisible).isEqualTo(true)
    }

    @Test
    fun `announcement details are loaded after start`() {
        assertThat(uiModelResults.isNotEmpty()).isEqualTo(true)
        assertThat(uiModelResults[1].isProgressVisible).isEqualTo(false)
        assertThat(uiModelResults[1].appVersion).isEqualTo("14.7")
        verify(featuresObserver).onChanged(testFeatures)
    }

    @Test
    fun `pressing close button closes the dialog`() {
        viewModel.onCloseDialogButtonPressed()
        verify(onDialogClosedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `pressing Find Out More triggers request for announcement details with specific URL`() {
        viewModel.onFindMoreButtonPressed()
        verify(onAnnouncementDetailsRequestedObserver).onChanged("https://wordpress.org/")
        verify(analyticsTrackerWrapper).track(Stat.FEATURE_ANNOUNCEMENT_FIND_OUT_MORE_TAPPED)
    }

    @Test
    fun `screen time is tracked when session ends`() {
        viewModel.onSessionEnded()
        verify(analyticsTrackerWrapper).track(
                eq(
                        Stat.FEATURE_ANNOUNCEMENT_CLOSE_DIALOG_BUTTON_TAPPED
                ),
                any<Map<String, *>>()
        )
    }
}
