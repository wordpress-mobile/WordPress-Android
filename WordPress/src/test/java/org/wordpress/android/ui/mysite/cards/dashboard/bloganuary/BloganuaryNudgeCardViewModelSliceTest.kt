package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.BloganuaryNudgeFeatureConfig

@OptIn(ExperimentalCoroutinesApi::class)
class BloganuaryNudgeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var bloganuaryNudgeFeatureConfig: BloganuaryNudgeFeatureConfig

    @Mock
    lateinit var bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    lateinit var viewModel: BloganuaryNudgeCardViewModelSlice

    @Before
    fun setUp() {
        viewModel = BloganuaryNudgeCardViewModelSlice(
            bloganuaryNudgeFeatureConfig,
            bloggingPromptsSettingsHelper,
            selectedSiteRepository,
            appPrefsWrapper,
        )
        viewModel.initialize(testScope())
    }

    @Test
    fun `GIVEN FF disabled, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(false)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN prompts not available, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(false)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN no selected site, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN card was hidden by user, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        whenever(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(true)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN FF enabled, prompts available, and card not hidden, WHEN getting builder params, THEN eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        whenever(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(false)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isTrue
    }

    @Test
    fun `GIVEN builder params, WHEN calling onLearnMoreClick, THEN navigate to overlay`() = test {
        val isPromptsEnabled = true
        whenever(bloggingPromptsSettingsHelper.isPromptsSettingEnabled()).thenReturn(isPromptsEnabled)

        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        whenever(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(false)

        val params = viewModel.getBuilderParams()
        params.onLearnMoreClick.invoke()

        advanceUntilIdle()
        assertThat(viewModel.onNavigation.value?.peekContent()).isEqualTo(
            SiteNavigationAction.OpenBloganuaryNudgeOverlay(isPromptsEnabled)
        )
    }

    @Test
    fun `GIVEN builder params, WHEN calling onHideMenuItemClick, THEN hide card in AppPrefs and refresh`() = test {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        whenever(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(false)

        val params = viewModel.getBuilderParams()
        params.onHideMenuItemClick.invoke()

        advanceUntilIdle()
        verify(appPrefsWrapper).setShouldHideBloganuaryNudgeCard(SITE_ID, true)
        assertThat(viewModel.refresh.value?.peekContent()).isTrue
    }

    // TODO thomashortadev: test onHideMenuItemClick when it is implemented

    companion object {
        private const val SITE_ID = 1L
        private val mockSiteModel: SiteModel = mock {
            on { siteId } doReturn SITE_ID
        }
    }
}
