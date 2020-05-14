package org.wordpress.android.viewmodel.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_PAGE
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.whatsnew.FeatureAnnouncement
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class WPMainActivityViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: WPMainActivityViewModel

    @Mock private lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var featureAnnouncementProvider: FeatureAnnouncementProvider
    @Mock lateinit var onFeatureAnnouncementRequestedObserver: Observer<Unit>
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private val featureAnnouncement = FeatureAnnouncement(
            "14.7",
            2,
            "https://wordpress.org/",
            emptyList()
    )

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(false)
        whenever(buildConfigWrapper.getAppVersionCode()).thenReturn(850)
        viewModel = WPMainActivityViewModel(
                featureAnnouncementProvider,
                buildConfigWrapper,
                appPrefsWrapper,
                analyticsTrackerWrapper
        )
        viewModel.onFeatureAnnouncementRequested.observeForever(
                onFeatureAnnouncementRequestedObserver
        )
    }

    @Test
    fun `fab visible when asked`() {
        startViewModelWithDefaultParameters()
        viewModel.onPageChanged(showFab = true, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabVisible).isEqualTo(true)
    }

    @Test
    fun `fab hidden when asked`() {
        startViewModelWithDefaultParameters()
        viewModel.onPageChanged(showFab = false, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip visible when asked`() {
        startViewModelWithDefaultParameters()
        viewModel.onPageChanged(showFab = true, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(true)
    }

    @Test
    fun `fab tooltip hidden when asked`() {
        startViewModelWithDefaultParameters()
        viewModel.onPageChanged(showFab = false, hasFullAccessToContent = true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when tapped`() {
        startViewModelWithDefaultParameters()
        viewModel.onTooltipTapped(true)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when user without full access to content uses the fab`() {
        startViewModelWithDefaultParameters()
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(false)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when bottom sheet opened`() {
        startViewModelWithDefaultParameters()
        whenever(appPrefsWrapper.isMainFabTooltipDisabled()).thenReturn(true)
        viewModel.onFabClicked(true)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `fab tooltip disabled when fab long pressed`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabLongPressed(true)
        verify(appPrefsWrapper).setMainFabTooltipDisabled(true)
        assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isEqualTo(false)
    }

    @Test
    fun `bottom sheet action is new post when new post is tapped`() {
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_POST } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_POST)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet action is new page when new page is tapped`() {
        startViewModelWithDefaultParameters()
        val action = viewModel.mainActions.value?.first { it.actionType == CREATE_NEW_PAGE } as CreateAction
        assertThat(action).isNotNull
        action.onClickAction?.invoke(CREATE_NEW_PAGE)
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_PAGE)
    }

    @Test
    fun `bottom sheet is visualized when user has full access to content`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(hasFullAccessToContent = true)
        assertThat(viewModel.createAction.value).isNull()
        assertThat(viewModel.isBottomSheetShowing.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `new post action is triggered from FAB when user has not full access to content`() {
        startViewModelWithDefaultParameters()
        viewModel.onFabClicked(hasFullAccessToContent = false)
        assertThat(viewModel.isBottomSheetShowing.value).isNull()
        assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `when user taps to open the login page from the bottom sheet empty view cta the correct action is triggered`() {
        startViewModelWithDefaultParameters()
        viewModel.onOpenLoginPage()

        assertThat(viewModel.startLoginFlow.value!!.peekContent()).isEqualTo(true)
    }

    @Test
    fun `onResume set expected content message when user has full access to content`() {
        startViewModelWithDefaultParameters()
        viewModel.onResume(true)
        assertThat(viewModel.fabUiState.value!!.CreateContentMessageId).isEqualTo(R.string.create_post_page_fab_tooltip)
    }

    @Test
    fun `onResume set expected content message when user has not full access to content`() {
        startViewModelWithDefaultParameters()
        viewModel.onResume(false)
        assertThat(viewModel.fabUiState.value!!.CreateContentMessageId)
                .isEqualTo(R.string.create_post_page_fab_tooltip_contributors)
    }

    @Test
    fun `show feature announcement when it's available and no announcement was not shown before`() {
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(-1)
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement()).thenReturn(
                featureAnnouncement
        )
        whenever(featureAnnouncementProvider.isFeatureAnnouncementAvailable()).thenReturn(true)

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())
        verify(analyticsTrackerWrapper).track(Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE)
    }

    @Test
    fun `show feature announcement when it's available and was not shown before`() {
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(1)
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement()).thenReturn(
                featureAnnouncement
        )
        whenever(featureAnnouncementProvider.isFeatureAnnouncementAvailable()).thenReturn(true)

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())
        verify(analyticsTrackerWrapper).track(Stat.FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE)
    }

    @Test
    fun `don't show feature announcement on fresh app install`() {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(0)

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
        verify(appPrefsWrapper).lastFeatureAnnouncementAppVersionCode = 850
    }

    @Test
    fun `don't show feature announcement when it's not available`() {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(featureAnnouncementProvider.isFeatureAnnouncementAvailable()).thenReturn(false)

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `don't show feature announcement when it's available but previous announcement is the same as current`() {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(2)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement()).thenReturn(
                featureAnnouncement
        )
        whenever(featureAnnouncementProvider.isFeatureAnnouncementAvailable()).thenReturn(true)

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, never()).onChanged(anyOrNull())
    }

    @Test
    fun `don't show feature announcement after view model starts again`() {
        whenever(appPrefsWrapper.lastFeatureAnnouncementAppVersionCode).thenReturn(840)
        whenever(appPrefsWrapper.featureAnnouncementShownVersion).thenReturn(-1)
        whenever(featureAnnouncementProvider.getLatestFeatureAnnouncement()).thenReturn(
                featureAnnouncement
        )
        whenever(featureAnnouncementProvider.isFeatureAnnouncementAvailable()).thenReturn(true)

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver).onChanged(anyOrNull())

        startViewModelWithDefaultParameters()

        verify(onFeatureAnnouncementRequestedObserver, times(1)).onChanged(anyOrNull())
    }

    private fun startViewModelWithDefaultParameters() {
        viewModel.start(isFabVisible = true, hasFullAccessToContent = true)
    }
}
