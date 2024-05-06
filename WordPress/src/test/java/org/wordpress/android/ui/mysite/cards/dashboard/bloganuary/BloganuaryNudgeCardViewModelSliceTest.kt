package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import android.icu.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker
import org.wordpress.android.ui.bloganuary.BloganuaryNudgeAnalyticsTracker.BloganuaryNudgeCardMenuItem
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.DateTimeUtilsWrapper
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

    @Mock
    lateinit var tracker: BloganuaryNudgeAnalyticsTracker

    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    lateinit var bloganuaryNudgeCardBuilder: BloganuaryNudgeCardBuilder

    lateinit var viewModel: BloganuaryNudgeCardViewModelSlice

    private var uiModels = mutableListOf<MySiteCardAndItem.Card.BloganuaryNudgeCardModel?>()

    @Before
    fun setUp() {
        viewModel = BloganuaryNudgeCardViewModelSlice(
            bloganuaryNudgeFeatureConfig,
            bloggingPromptsSettingsHelper,
            selectedSiteRepository,
            appPrefsWrapper,
            tracker,
            dateTimeUtilsWrapper,
            bloganuaryNudgeCardBuilder
        )
        viewModel.initialize(testScope())

        viewModel.uiModel.observeForever { uiModel ->
            uiModels.add(uiModel)
        }
    }

    @Test
    fun `GIVEN FF disabled, WHEN getting builder params, THEN not eligible`() {
        mockCalendarMonth(Calendar.DECEMBER)
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(false)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN not December, WHEN getting builder params, THEN not eligible`() {
        // need to use it to make sure that test will fail if other month meets the requirement incorrectly
        val lenient = Mockito.lenient()

        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        lenient.`when`(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        lenient.`when`(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        lenient.`when`(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(false)

        // Test all months except eligible months
        (Calendar.FEBRUARY..Calendar.NOVEMBER).forEach { month ->
            mockCalendarMonth(month)

            val params = viewModel.getBuilderParams()

            assertThat(params.isEligible).isFalse
            Mockito.reset(dateTimeUtilsWrapper)
        }
    }

    @Test
    fun `GIVEN prompts not available, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        mockCalendarMonth(Calendar.DECEMBER)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(false)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN no selected site, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        mockCalendarMonth(Calendar.DECEMBER)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN card was hidden by user, WHEN getting builder params, THEN not eligible`() {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        mockCalendarMonth(Calendar.DECEMBER)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        whenever(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(true)

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isFalse
    }

    @Test
    fun `GIVEN requirements met, WHEN getting builder params, THEN eligible`() {
        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()

        assertThat(params.isEligible).isTrue
    }

    @Test
    fun `GIVEN requirements met AND month is DECEMBER, WHEN getting builder params, THEN return correct title`() {
        mockEligibleRequirements(Calendar.DECEMBER)

        val params = viewModel.getBuilderParams()

        assertThat(params.title).isEqualTo(UiStringRes(R.string.bloganuary_dashboard_nudge_title_december))
    }

    @Test
    fun `GIVEN requirements met AND month is JANUARY, WHEN getting builder params, THEN return correct title`() {
        mockEligibleRequirements(Calendar.JANUARY)

        val params = viewModel.getBuilderParams()

        assertThat(params.title).isEqualTo(UiStringRes(R.string.bloganuary_dashboard_nudge_title_january))
    }

    @Test
    fun `GIVEN requirements met, WHEN getting builder params, THEN return correct text`() {
        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()

        assertThat(params.text).isEqualTo(UiStringRes(R.string.bloganuary_dashboard_nudge_text))
    }

    @Test
    fun `GIVEN builder params, WHEN calling onLearnMoreClick, THEN navigate to overlay`() = test {
        val isPromptsEnabled = true
        whenever(bloggingPromptsSettingsHelper.isPromptsSettingEnabled()).thenReturn(isPromptsEnabled)

        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()
        params.onLearnMoreClick.invoke()

        advanceUntilIdle()
        assertThat(viewModel.onNavigation.value?.peekContent()).isEqualTo(
            SiteNavigationAction.OpenBloganuaryNudgeOverlay(isPromptsEnabled)
        )
    }

    @Test
    fun `GIVEN builder params, WHEN calling onHideMenuItemClick, THEN hide card in AppPrefs and refresh`() = test {
        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()
        params.onHideMenuItemClick.invoke()

        advanceUntilIdle()
        verify(appPrefsWrapper).setShouldHideBloganuaryNudgeCard(SITE_ID, true)
        assertThat(uiModels.last()).isNull()
    }

    // region Analytics
    @Test
    fun `GIVEN builder params, WHEN calling onLearnMoreClick, THEN track analytics`() = test {
        val isPromptsEnabled = true
        whenever(bloggingPromptsSettingsHelper.isPromptsSettingEnabled()).thenReturn(isPromptsEnabled)

        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()
        params.onLearnMoreClick.invoke()

        advanceUntilIdle()
        verify(tracker).trackMySiteCardLearnMoreTapped(isPromptsEnabled)
    }

    @Test
    fun `GIVEN builder params, WHEN calling onMoreMenuClick, THEN track analytics`() = test {
        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()
        params.onMoreMenuClick.invoke()

        advanceUntilIdle()
        verify(tracker).trackMySiteCardMoreMenuTapped()
    }

    @Test
    fun `GIVEN builder params, WHEN calling onHideMenuItemClick, THEN track analytics`() = test {
        mockEligibleRequirements()

        val params = viewModel.getBuilderParams()
        params.onHideMenuItemClick.invoke()

        advanceUntilIdle()
        verify(tracker).trackMySiteCardMoreMenuItemTapped(BloganuaryNudgeCardMenuItem.HIDE_THIS)
    }
    // endregion

    private fun mockCalendarMonth(month: Int) {
        val mockCalendar: Calendar = mock {
            on { get(Calendar.MONTH) } doReturn month
        }
        whenever(dateTimeUtilsWrapper.getCalendarInstance()).thenReturn(mockCalendar)
    }

    private fun mockEligibleRequirements(month: Int = Calendar.DECEMBER) {
        whenever(bloganuaryNudgeFeatureConfig.isEnabled()).thenReturn(true)
        mockCalendarMonth(month)
        whenever(bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mockSiteModel)
        whenever(appPrefsWrapper.getShouldHideBloganuaryNudgeCard(SITE_ID)).thenReturn(false)
    }

    companion object {
        private const val SITE_ID = 1L
        private val mockSiteModel: SiteModel = mock {
            on { siteId } doReturn SITE_ID
        }
    }
}
