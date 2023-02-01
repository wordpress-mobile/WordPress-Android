package org.wordpress.android.ui.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig
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
    lateinit var bloggingPromptsFeatureConfig: BloggingPromptsFeatureConfig

    @Mock
    lateinit var bloggingPromptsEnhancementsFeatureConfig: BloggingPromptsEnhancementsFeatureConfig

    lateinit var helper: BloggingPromptsSettingsHelper

    @Before
    fun setUp() {
        helper = BloggingPromptsSettingsHelper(
            bloggingRemindersStore,
            selectedSiteRepository,
            appPrefsWrapper,
            bloggingPromptsFeatureConfig,
            bloggingPromptsEnhancementsFeatureConfig,
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
    fun `given prompts FF is off and site is potential blog, when isPromptsFeatureAvailable, then returns false`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
            SiteModel().apply { setIsPotentialBloggingSite(true) }
        )

        val result = helper.isPromptsFeatureAvailable()

        assertThat(result).isFalse
    }

    @Test
    fun `given prompts FF is on and site is not potential blog, when isPromptsFeatureAvailable, then returns false`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
            SiteModel().apply { setIsPotentialBloggingSite(false) }
        )

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
    fun `given prompts FF is on and site is potential blog, when isPromptsFeatureAvailable, then returns true`() {
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
            SiteModel().apply { setIsPotentialBloggingSite(true) }
        )

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
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(false)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
            SiteModel().apply {
                id = 123
                setIsPotentialBloggingSite(true)
            }
        )

        val result = helper.shouldShowPromptsFeature()

        assertThat(result).isFalse
    }

    @Test
    fun `given enhancements FF on and prompts setting off, when isPromptsFeatureActive, then returns false`() = test {
        // prompts feature is available
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
            SiteModel().apply {
                id = 123
                setIsPotentialBloggingSite(true)
            }
        )

        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(true)

        val model = createRemindersModel(isPromptsCardEnabled = false)
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
            flowOf(model)
        }

        val result = helper.shouldShowPromptsFeature()

        assertThat(result).isFalse
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given enhancements FF on and prompts setting on and skipped for today, when isPromptsFeatureActive, then returns false`() =
        test {
            // prompts feature is available
            whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
                SiteModel().apply {
                    id = 123
                    setIsPotentialBloggingSite(true)
                }
            )

            whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(true)

            val model = createRemindersModel(isPromptsCardEnabled = true)
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
                flowOf(model)
            }

            whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(Date())

            val result = helper.shouldShowPromptsFeature()

            assertThat(result).isFalse
        }

    @Test
    fun `given enhancements FF off and skipped for today, when isPromptsFeatureActive, then returns false`() = test {
        // prompts feature is available
        whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
            SiteModel().apply {
                id = 123
                setIsPotentialBloggingSite(true)
            }
        )

        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(false)

        whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(Date())

        val result = helper.shouldShowPromptsFeature()

        assertThat(result).isFalse
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given enhancements FF on and prompts setting on and not skipped for today, when isPromptsFeatureActive, then returns false`() =
        test {
            // prompts feature is available
            whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
                SiteModel().apply {
                    id = 123
                    setIsPotentialBloggingSite(true)
                }
            )

            whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(true)

            val model = createRemindersModel(isPromptsCardEnabled = true)
            whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
                flowOf(model)
            }

            whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(null)

            val result = helper.shouldShowPromptsFeature()

            assertThat(result).isTrue
        }

    @Suppress("MaxLineLength")
    @Test
    fun `given enhancements FF off and not skipped for today, when isPromptsFeatureActive, then returns false`() =
        test {
            // prompts feature is available
            whenever(bloggingPromptsFeatureConfig.isEnabled()).thenReturn(true)
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(
                SiteModel().apply {
                    id = 123
                    setIsPotentialBloggingSite(true)
                }
            )

            whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(false)

            whenever(appPrefsWrapper.getSkippedPromptDay(any())).thenReturn(null)

            val result = helper.shouldShowPromptsFeature()

            assertThat(result).isTrue
        }

    companion object {
        private fun createRemindersModel(
            isPromptsCardEnabled: Boolean,
        ) = BloggingRemindersModel(
            siteId = 123,
            isPromptsCardEnabled = isPromptsCardEnabled,
        )
    }
}
