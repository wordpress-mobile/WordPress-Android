package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.util.config.BloganuaryNudgeFeatureConfig

@OptIn(ExperimentalCoroutinesApi::class)
class BloganuaryNudgeCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var bloganuaryNudgeFeatureConfig: BloganuaryNudgeFeatureConfig

    @Mock
    lateinit var bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper

    lateinit var viewModel: BloganuaryNudgeCardViewModelSlice

    @Before
    fun setUp() {
        viewModel = BloganuaryNudgeCardViewModelSlice(
            bloganuaryNudgeFeatureConfig,
            bloggingPromptsSettingsHelper
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
    fun `GIVEN FF enabled and prompts available, WHEN getting builder params, THEN eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isTrue
    }

    @Test
    fun `GIVEN builder params, WHEN calling onLearnMoreClick, THEN navigate to overlay`() = test {
        val isPromptsEnabled = true
        whenever(bloggingPromptsSettingsHelper.isPromptsSettingEnabled()).thenReturn(isPromptsEnabled)

        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)

        val params = viewModel.getBuilderParams()
        params.onLearnMoreClick.invoke()

        advanceUntilIdle()
        assertThat(viewModel.onNavigation.value?.peekContent()).isEqualTo(
            SiteNavigationAction.OpenBloganuaryNudgeOverlay(isPromptsEnabled)
        )
    }

    // TODO thomashortadev: test onMoreMenuClick and onHideMenuItemClick when they are implemented
}
