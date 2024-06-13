package org.wordpress.android.ui.main.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.main.WPMainNavigationView.PageType
import org.wordpress.android.ui.voicetocontent.VoiceToContentFeatureUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.ReaderFloatingButtonFeatureConfig
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainCreateSheetHelperTest : BaseUnitTest() {
    @Mock
    private lateinit var voiceToContentFeatureUtils: VoiceToContentFeatureUtils

    @Mock
    private lateinit var readerFloatingButtonFeatureConfig: ReaderFloatingButtonFeatureConfig

    @Mock
    private lateinit var bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper

    @Mock
    private lateinit var buildConfig: BuildConfigWrapper

    @Mock
    private lateinit var siteUtils: SiteUtilsWrapper

    private lateinit var helper: MainCreateSheetHelper

    @Before
    fun setUp() {
        helper = MainCreateSheetHelper(
            voiceToContentFeatureUtils,
            readerFloatingButtonFeatureConfig,
            bloggingPromptsSettingsHelper,
            buildConfig,
            siteUtils,
        )
    }

    // region shouldShowFabForPage
    @Test
    fun `shouldShowFabForPage returns true for my site page`() {
        // Arrange
        val page = PageType.MY_SITE
        whenever(buildConfig.isCreateFabEnabled).thenReturn(true)

        // Act
        val result = helper.shouldShowFabForPage(page)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `shouldShowFabForPage returns true for reader page when reader floating button feature is enabled`() {
        // Arrange
        val page = PageType.READER
        whenever(buildConfig.isCreateFabEnabled).thenReturn(true)
        whenever(readerFloatingButtonFeatureConfig.isEnabled()).thenReturn(true)

        // Act
        val result = helper.shouldShowFabForPage(page)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `shouldShowFabForPage returns false for reader page when reader floating button feature is disabled`() {
        // Arrange
        val page = PageType.READER
        whenever(buildConfig.isCreateFabEnabled).thenReturn(true)
        whenever(readerFloatingButtonFeatureConfig.isEnabled()).thenReturn(false)

        // Act
        val result = helper.shouldShowFabForPage(page)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `shouldShowFabForPage returns false for my site page when create fab is disabled`() {
        // Arrange
        val page = PageType.MY_SITE
        whenever(buildConfig.isCreateFabEnabled).thenReturn(false)

        // Act
        val result = helper.shouldShowFabForPage(page)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `shouldShowFabForPage returns false for reader page when create fab is disabled`() {
        // Arrange
        val page = PageType.READER
        whenever(buildConfig.isCreateFabEnabled).thenReturn(false)
        whenever(readerFloatingButtonFeatureConfig.isEnabled()).thenReturn(true)

        // Act
        val result = helper.shouldShowFabForPage(page)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `shouldShowFabForPage returns false for other pages`() {
        PageType.entries
            .filterNot { it == PageType.MY_SITE || it == PageType.READER }
            .forEach { page ->
                // Arrange
                whenever(buildConfig.isCreateFabEnabled).thenReturn(true)

                // Act
                val result = helper.shouldShowFabForPage(page)

                // Assert
                assertThat(result).isFalse()
            }
    }
    // endregion

    // region canCreatePost
    @Test
    fun `canCreatePost returns true`() {
        // Act
        val result = helper.canCreatePost()

        // Assert
        assertThat(result).isTrue()
    }
    // endregion

    // region canCreatePage
    @Test
    fun `canCreatePage returns true for my site page with full access to content`() {
        // Arrange
        val site = SiteModel()
        val page = PageType.MY_SITE
        whenever(siteUtils.hasFullAccessToContent(site)).thenReturn(true)

        // Act
        val result = helper.canCreatePage(site, page)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `canCreatePage returns false for my site page without full access to content`() {
        // Arrange
        val site = SiteModel()
        val page = PageType.MY_SITE
        whenever(siteUtils.hasFullAccessToContent(site)).thenReturn(false)

        // Act
        val result = helper.canCreatePage(site, page)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `canCreatePage returns false for other pages with full access to content`() {
        PageType.entries
            .filterNot { it == PageType.MY_SITE }
            .forEach { page ->
                // Arrange
                val site = SiteModel()
                whenever(siteUtils.hasFullAccessToContent(site)).thenReturn(true)

                // Act
                val result = helper.canCreatePage(site, page)

                // Assert
                assertThat(result).isFalse()
            }
    }
    // endregion

    // region canCreatePostFromAudio
    @Test
    fun `canCreatePostFromAudio returns true when voice to content is enabled and site has full access to content`() {
        // Arrange
        val site = SiteModel()
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(true)
        whenever(siteUtils.hasFullAccessToContent(site)).thenReturn(true)

        // Act
        val result = helper.canCreatePostFromAudio(site)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `canCreatePostFromAudio returns false when voice to content is disabled`() {
        // Arrange
        val site = SiteModel()
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(false)

        // Act
        val result = helper.canCreatePostFromAudio(site)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `canCreatePostFromAudio returns false when site does not have full access to content`() {
        // Arrange
        val site = SiteModel()
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(true)
        whenever(siteUtils.hasFullAccessToContent(site)).thenReturn(false)

        // Act
        val result = helper.canCreatePostFromAudio(site)

        // Assert
        assertThat(result).isFalse()
    }
    // endregion

    // region canCreatePromptAnswer
    @Test
    fun `canCreatePromptAnswer returns true when prompts feature should be shown`() = test {
        // Arrange
        whenever(bloggingPromptsSettingsHelper.shouldShowPromptsFeature()).thenReturn(true)

        // Act
        val result = helper.canCreatePromptAnswer()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `canCreatePromptAnswer returns false when prompts feature should not be shown`() = test {
        // Arrange
        whenever(bloggingPromptsSettingsHelper.shouldShowPromptsFeature()).thenReturn(false)

        // Act
        val result = helper.canCreatePromptAnswer()

        // Assert
        assertThat(result).isFalse()
    }
    // endregion
}
