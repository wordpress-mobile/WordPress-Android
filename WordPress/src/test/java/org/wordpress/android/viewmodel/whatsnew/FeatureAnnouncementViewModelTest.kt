package org.wordpress.android.viewmodel.whatsnew

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementItem
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel.FeatureAnnouncementUiModel
import org.wordpress.android.util.NoDelayCoroutineDispatcher

class FeatureAnnouncementViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: FeatureAnnouncementViewModel
    @Mock lateinit var onDialogClosedObserver: Observer<Unit>
    @Mock lateinit var onAnnouncementDetailsRequestedObserver: Observer<String>
    @Mock lateinit var featuresObserver: Observer<List<FeatureAnnouncementItem>>
    @Mock lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider

    private val uiModelResults = mutableListOf<FeatureAnnouncementUiModel>()

    private val testFeatures = listOf(
            FeatureAnnouncementItem(
                    "Test Feature 1",
                    "Test Description 1",
                    1
            ),
            FeatureAnnouncementItem(
                    "Test Feature 2",
                    "Test Description 1",
                    2
            ),
            FeatureAnnouncementItem(
                    "Test Feature 3",
                    "Test Description 3",
                    3
            )
    )

    private val featureAnnouncement = FeatureAnnouncement(
            "14.7",
            850,
            "https://wordpress.org/",
            testFeatures
    )

    @Before
    fun setUp() {
        uiModelResults.clear()

        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement()).thenReturn(featureAnnouncement)

        viewModel = FeatureAnnouncementViewModel(featureAnnouncementProvider, NoDelayCoroutineDispatcher())
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
    }
}
