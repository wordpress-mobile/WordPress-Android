package org.wordpress.android.ui.review

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.eventToList
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.InAppReviewsFeatureConfig
import kotlin.test.assertEquals

@RunWith(MockitoJUnitRunner::class)
class ReviewViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var inAppReviewsFeatureConfig: InAppReviewsFeatureConfig

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var viewModel: ReviewViewModel

    private lateinit var events: MutableList<Unit>

    @Before
    fun setup() {
        whenever(inAppReviewsFeatureConfig.isEnabled()).thenReturn(true)
        viewModel = ReviewViewModel(appPrefsWrapper, inAppReviewsFeatureConfig)
        events = mutableListOf()
        events = viewModel.launchReview.eventToList()
    }

    @Test
    fun onPublishingPost_whenPublishedCountIsLow_doNotLaunchInAppReviews() {
        whenever(appPrefsWrapper.isInAppReviewsShown()).thenReturn(false)
        whenever(appPrefsWrapper.getPublishedPostCount()).thenReturn(ReviewViewModel.TARGET_COUNT_POST_PUBLISHED - 1)

        viewModel.onPublishingPost(true)

        assertEquals(events.size, 0)
    }

    @Test
    fun onPublishingPost_whenInAppReviewsAlreadyShown_doNotLaunchInAppReviews() {
        whenever(appPrefsWrapper.isInAppReviewsShown()).thenReturn(true)

        viewModel.onPublishingPost(true)

        assertEquals(events.size, 0)
    }

    @Test
    fun onPublishingPost_whenPublishedCountIsHigh_launchInAppReviews() {
        whenever(appPrefsWrapper.isInAppReviewsShown()).thenReturn(false)
        whenever(appPrefsWrapper.getPublishedPostCount()).thenReturn(ReviewViewModel.TARGET_COUNT_POST_PUBLISHED)

        viewModel.onPublishingPost(true)

        // Verify `launchReview` is triggered.
        assertEquals(Unit, events.last())
    }
}
