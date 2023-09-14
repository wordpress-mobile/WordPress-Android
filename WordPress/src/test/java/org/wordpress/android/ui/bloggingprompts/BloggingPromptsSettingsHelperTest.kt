package org.wordpress.android.ui.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.BloggingPromptsFeature
import java.util.Date

@ExperimentalCoroutinesApi
class BloggingPromptsSettingsHelperTest : BaseUnitTest() {
    @Mock
    lateinit var bloggingRemindersStore: BloggingRemindersStore

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var bloggingPromptsFeature: BloggingPromptsFeature

    @Mock
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var helper: BloggingPromptsSettingsHelper

    @Before
    fun setUp() {
        helper = BloggingPromptsSettingsHelper(
            bloggingRemindersStore,
            selectedSiteRepository,
            appPrefsWrapper,
            bloggingPromptsFeature,
            analyticsTracker
        )
    }

    @Test
    fun `when getPromptsCardEnabledLiveData, then returns the store model isPromptsCardEnabled value`() {
        val expectedState = true
        val model = createRemindersModel(isPromptsCardEnabled = expectedState)
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
            flowOf(model)
        }

        var result: Boolean? = null
        helper.getPromptsCardEnabledLiveData(123).observeForever { result = it }

        assertThat(result).isEqualTo(expectedState)
    }

    @Test
    fun `when updatePromptsCardEnabledBlocking, then calls suspending version internally`() {
        val spyHelper = spy(helper)

        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
            flowOf()
        }

        spyHelper.updatePromptsCardEnabledBlocking(123, isEnabled = true)

        verifyBlocking(spyHelper) {
            updatePromptsCardEnabled(any(), any())
        }
    }

    @Test
    fun `when updatePromptsCardEnabled, then updates the store model`() = test {
        val expectedState = true
        val model = createRemindersModel(isPromptsCardEnabled = false)
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
            flowOf(model)
        }

        helper.updatePromptsCardEnabled(123, isEnabled = expectedState)

        verify(bloggingRemindersStore).updateBloggingReminders(argThat { isPromptsCardEnabled == expectedState })
    }

    @Test
    fun `given prompts FF is off and site is wpcom site, when isPromptsFeatureAvailable, then returns false`() {
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

        val result = helper.isPromptsFeatureAvailable()

        assertThat(result).isFalse
    }

    @Test
    fun `given prompts FF is on and site is not wpcom site, when isPromptsFeatureAvailable, then returns false`() {
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel(isWpComSite = false))

        val result = helper.isPromptsFeatureAvailable()

        assertThat(result).isFalse
    }

    @Test
    fun `given site is not selected, when isPromptsFeatureAvailable, then returns false`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val result = helper.isPromptsFeatureAvailable()

        assertThat(result).isFalse
    }

    @Test
    fun `given prompts FF is on and site is wpcom site, when isPromptsFeatureAvailable, then returns true`() {
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

        val result = helper.isPromptsFeatureAvailable()

        assertThat(result).isTrue
    }

    @Test
    fun `given site is not selected, when isPromptsFeatureActive, then returns false`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        val result = helper.shouldShowPromptsFeature()

        assertThat(result).isFalse
    }

    @Test
    fun `given prompts feature not available, when isPromptsFeatureActive, then returns false`() = test {
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

        val result = helper.shouldShowPromptsFeature()

        assertThat(result).isFalse
    }

    @Test
    fun `given prompts setting off, when isPromptsFeatureActive, then returns false`() = test {
        // prompts feature is available
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

        val model = createRemindersModel(isPromptsCardEnabled = false)
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
            flowOf(model)
        }

        val result = helper.shouldShowPromptsFeature()

        assertThat(result).isFalse
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given prompts setting on and skipped for today, when isPromptsFeatureActive, then returns false`() =
        test {
            // prompts feature is available
            whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

            val model = createRemindersModel(isPromptsCardEnabled = true)
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
                flowOf(model)
            }

            whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(Date())

            val result = helper.shouldShowPromptsFeature()

            assertThat(result).isFalse
        }


    @Suppress("MaxLineLength")
    @Test
    fun `given prompts setting on and not skipped for today, when isPromptsFeatureActive, then returns false`() =
        test {
            // prompts feature is available
            whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

            val model = createRemindersModel(isPromptsCardEnabled = true)
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
                flowOf(model)
            }

            whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(null)

            val result = helper.shouldShowPromptsFeature()

            assertThat(result).isTrue
        }


    @Test
    fun `when trackPromptsCardEnabledSettingTapped is called, then it tracks with correct properties`() {
        val isEnabled = true

        helper.trackPromptsCardEnabledSettingTapped(isEnabled)

        verify(analyticsTracker)
            .track(
                eq(AnalyticsTracker.Stat.BLOGGING_PROMPTS_SETTINGS_SHOW_PROMPTS_TAPPED),
                argWhere<Map<String, Any?>> {
                    assertThat(it).hasSize(1)
                    assertThat(it).containsEntry("enabled", isEnabled)
                    true
                }
            )
    }

    @Test
    fun `given prompts feature is not available, when shouldShowPromptsSetting, then returns false`() {
        // prompts available
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel(isWpComSite = false))

        val result = helper.shouldShowPromptsSetting()

        assertThat(result).isFalse
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given prompts feature is available and enhancements FF is on, when shouldShowPromptsSetting, then returns true`() {
        // prompts available
        whenever(bloggingPromptsFeature.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(createSiteModel())

        val result = helper.shouldShowPromptsSetting()

        assertThat(result).isTrue()
    }

    companion object {
        private fun createRemindersModel(
            isPromptsCardEnabled: Boolean,
        ) = BloggingRemindersModel(
            siteId = 123,
            isPromptsCardEnabled = isPromptsCardEnabled,
        )

        private fun createSiteModel(
            isWpComSite: Boolean = true
        ) = SiteModel().apply {
            this.id = 123
            setIsWPCom(isWpComSite)
        }
    }
}
