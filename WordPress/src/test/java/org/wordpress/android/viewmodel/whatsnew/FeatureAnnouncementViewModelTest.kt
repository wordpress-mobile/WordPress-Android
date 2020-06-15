package org.wordpress.android.viewmodel.whatsnew

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel.FeatureAnnouncementUiModel
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NoDelayCoroutineDispatcher
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class FeatureAnnouncementViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: FeatureAnnouncementViewModel
    @Mock lateinit var onDialogClosedObserver: Observer<Unit>
    @Mock lateinit var onAnnouncementDetailsRequestedObserver: Observer<String>
    @Mock lateinit var featuresObserver: Observer<List<FeatureAnnouncementItem>>
    @Mock lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper

    private val uiModelResults = mutableListOf<FeatureAnnouncementUiModel>()

    private val testFeatures = listOf(
            FeatureAnnouncementItem(
                    "Test Feature 1",
                    "Test Description 1",
                    "",
                    "https://wordpress.org/icon1.png"
            ),
            FeatureAnnouncementItem(
                    "Test Feature 2",
                    "Test Description 1",
                    "",
                    "https://wordpress.org/icon2.png"
            ),
            FeatureAnnouncementItem(
                    "Test Feature 3",
                    "Test Description 3",
                    "",
                    "https://wordpress.org/icon3.png"
            )
    )

    private val featureAnnouncement = FeatureAnnouncement(
            "14.7",
            1,
            "14.5",
            "14.7",
            "https://wordpress.org/",
            true,
            testFeatures
    )

    @Before
    fun setUp() = runBlockingTest {
        uiModelResults.clear()

        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(any())).thenReturn(featureAnnouncement)
        whenever(buildConfigWrapper.getAppVersionCode()).thenReturn(850)
        viewModel = FeatureAnnouncementViewModel(
                featureAnnouncementProvider,
                analyticsTrackerWrapper,
                buildConfigWrapper,
                appPrefsWrapper,
                NoDelayCoroutineDispatcher()
        )
        viewModel.uiModel.observeForever { if (it != null) uiModelResults.add(it) }
        viewModel.onDialogClosed.observeForever(onDialogClosedObserver)
        viewModel.onAnnouncementDetailsRequested.observeForever(onAnnouncementDetailsRequestedObserver)
        viewModel.featureItems.observeForever(featuresObserver)
    }

    @Test
    fun `progress and find out more is visible after start`() {
        viewModel.start()
        assertThat(uiModelResults.isNotEmpty()).isEqualTo(true)
        assertThat(uiModelResults[0].isProgressVisible).isEqualTo(true)
    }

    @Test
    fun `announcement details are loaded after start`() {
        viewModel.start()
        assertThat(uiModelResults.isNotEmpty()).isEqualTo(true)
        assertThat(uiModelResults[1].isProgressVisible).isEqualTo(false)
        assertThat(uiModelResults[1].appVersion).isEqualTo("14.7")
        verify(featuresObserver).onChanged(testFeatures)
    }

    @Test
    fun `pressing close button closes the dialog`() {
        viewModel.start()
        viewModel.onCloseDialogButtonPressed()
        verify(onDialogClosedObserver).onChanged(anyOrNull())
    }

    @Test
    fun `pressing Find Out More triggers request for announcement details with specific URL`() {
        viewModel.start()
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

    @Test
    fun `find Out More is  visible when detailsUrl is present`() {
        viewModel.start()
        assertThat(uiModelResults[1].isFindOutMoreVisible).isEqualTo(true)
    }

    @Test
    fun `find Out More is not visible when detailsUrl is missing`() = test {
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement(true)).thenReturn(
                featureAnnouncement.copy(detailsUrl = "")
        )

        viewModel.start()
        assertThat(uiModelResults[1].isFindOutMoreVisible).isEqualTo(false)
    }

    @Test
    fun `announcement and app versions are recorder as shown when announcement is diaplyed`() {
        viewModel.start()

        verify(appPrefsWrapper).featureAnnouncementShownVersion = 1
        verify(appPrefsWrapper).lastFeatureAnnouncementAppVersionCode = 850
    }

    @Test
    fun `when no cached announcement is available we will try to fetch one from endpoint`() = test {
        viewModel.start()

        verify(featureAnnouncementProvider).getLatestFeatureAnnouncement(true)
        verify(featureAnnouncementProvider).getLatestFeatureAnnouncement(false)
    }
}
