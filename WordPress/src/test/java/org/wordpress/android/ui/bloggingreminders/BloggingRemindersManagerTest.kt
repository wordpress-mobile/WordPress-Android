package org.wordpress.android.ui.bloggingreminders

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper

@RunWith(MockitoJUnitRunner::class)
class BloggingRemindersManagerTest {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private lateinit var bloggingRemindersManager: BloggingRemindersManager
    private val siteId = 123

    @Before
    fun setUp() {
        bloggingRemindersManager = BloggingRemindersManager(appPrefsWrapper, buildConfigWrapper)
    }

    @Test
    fun `should not show blogging reminders when it is not jetpack app`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isFalse
    }

    @Test
    fun `should show blogging reminders when it is jetpack app`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isTrue
    }

    @Test
    fun `should not show blogging reminders when already shown for a site`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(appPrefsWrapper.isBloggingRemindersShown(siteId)).thenReturn(true)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isFalse
    }

    @Test
    fun `should show blogging reminders when already not shown for a site`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        whenever(appPrefsWrapper.isBloggingRemindersShown(siteId)).thenReturn(false)

        val result = bloggingRemindersManager.shouldShowBloggingRemindersPrompt(siteId)

        assertThat(result).isTrue
    }
}
