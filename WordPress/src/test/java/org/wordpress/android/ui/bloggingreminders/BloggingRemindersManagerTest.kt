package org.wordpress.android.ui.bloggingreminders

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.BloggingRemindersFeatureConfig

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersManagerTest {
    @Mock lateinit var bloggingRemindersFeatureConfig: BloggingRemindersFeatureConfig
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var bloggingRemindersManager: BloggingRemindersManager
    private val siteId = 123

    @Before
    fun setUp() {
        bloggingRemindersManager = BloggingRemindersManager(bloggingRemindersFeatureConfig, appPrefsWrapper)
    }

    @Test
    fun `should not show blogging reminders when flag disabled`() {
        whenever(bloggingRemindersFeatureConfig.isEnabled()).thenReturn(false)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isFalse()
    }

    @Test
    fun `should not show blogging reminders when already shown for a site`() {
        whenever(bloggingRemindersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.isBloggingRemindersShown(siteId)).thenReturn(true)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isFalse()
    }

    @Test
    fun `should show blogging reminders when already not shown for a site and flag enabled`() {
        whenever(bloggingRemindersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(appPrefsWrapper.isBloggingRemindersShown(siteId)).thenReturn(false)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isTrue()
    }
}
