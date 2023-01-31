package org.wordpress.android.ui.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.store.BloggingRemindersStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig
import org.wordpress.android.util.config.BloggingPromptsFeatureConfig

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
    fun `when getPromptsCardEnabledLiveData then returns the store model isPromptsCardEnabled value`() {
        val model = createRemindersModel(isPromptIncluded = false, isPromptsCardEnabled = true)
        whenever(bloggingRemindersStore.bloggingRemindersModel(any())).doAnswer {
            flowOf(model)
        }

        var result: Boolean? = null
        helper.getPromptsCardEnabledLiveData(123).observeForever { result = it }

        assertThat(result).isTrue
    }

    companion object {
        private fun createRemindersModel(
            isPromptIncluded: Boolean,
            isPromptsCardEnabled: Boolean,
        ) = BloggingRemindersModel(
            siteId = 123,
            isPromptIncluded = isPromptIncluded,
            isPromptsCardEnabled = isPromptsCardEnabled,
        )
    }
}
