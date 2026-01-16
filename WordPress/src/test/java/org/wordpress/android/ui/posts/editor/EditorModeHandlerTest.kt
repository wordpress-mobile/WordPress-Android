package org.wordpress.android.ui.posts.editor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession

@RunWith(MockitoJUnitRunner.Silent::class)
class EditorModeHandlerTest {
    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var listener: EditorModeHandler.EditorModeListener

    @Mock
    private lateinit var editorFragment: EditorFragmentAbstract

    @Mock
    private lateinit var siteModel: SiteModel

    @Mock
    private lateinit var analyticsSession: PostEditorAnalyticsSession

    private lateinit var handler: EditorModeHandler

    @Before
    fun setUp() {
        handler = EditorModeHandler(dispatcher)
        handler.start(listener)

        whenever(listener.getEditorFragment()).thenReturn(editorFragment)
        whenever(listener.getSiteModel()).thenReturn(siteModel)
        whenever(listener.getPostEditorAnalyticsSession()).thenReturn(analyticsSession)
    }

    // region isHtmlModeOn tests
    @Test
    fun `initial htmlModeOn is false`() {
        assertThat(handler.isHtmlModeOn()).isFalse()
    }

    @Test
    fun `setHtmlModeOn updates state`() {
        handler.setHtmlModeOn(true)
        assertThat(handler.isHtmlModeOn()).isTrue()
    }

    @Test
    fun `setHtmlModeOn to false updates state`() {
        handler.setHtmlModeOn(true)
        handler.setHtmlModeOn(false)
        assertThat(handler.isHtmlModeOn()).isFalse()
    }
    // endregion

    // region toggleHtmlMode tests
    @Test
    fun `toggleHtmlMode switches from false to true`() {
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn("HTML mode")

        handler.toggleHtmlMode()

        assertThat(handler.isHtmlModeOn()).isTrue()
    }

    @Test
    fun `toggleHtmlMode switches from true to false`() {
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn("HTML mode")
        whenever(listener.getString(R.string.menu_visual_mode_switched_notice)).thenReturn("Visual mode")

        handler.setHtmlModeOn(true)
        handler.toggleHtmlMode()

        assertThat(handler.isHtmlModeOn()).isFalse()
    }

    @Test
    fun `toggleHtmlMode invalidates options menu`() {
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn("HTML mode")

        handler.toggleHtmlMode()

        verify(listener).invalidateOptionsMenu()
    }

    @Test
    fun `toggleHtmlMode shows notice on editor fragment`() {
        val htmlMessage = "HTML mode enabled"
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn(htmlMessage)

        handler.toggleHtmlMode()

        verify(editorFragment).showNotice(htmlMessage)
    }

    @Test
    fun `toggleHtmlMode shows visual notice when switching to visual`() {
        val visualMessage = "Visual mode enabled"
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn("HTML")
        whenever(listener.getString(R.string.menu_visual_mode_switched_notice)).thenReturn(visualMessage)

        handler.setHtmlModeOn(true)
        handler.toggleHtmlMode()

        verify(editorFragment).showNotice(visualMessage)
    }

    @Test
    fun `toggleHtmlMode tracks editor switch to HTML`() {
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn("HTML mode")

        handler.toggleHtmlMode()

        verify(analyticsSession).switchEditor(PostEditorAnalyticsSession.Editor.HTML)
    }

    @Test
    fun `toggleHtmlMode tracks editor switch to classic when switching back from HTML`() {
        whenever(listener.getString(R.string.menu_html_mode_switched_notice)).thenReturn("HTML")
        whenever(listener.getString(R.string.menu_visual_mode_switched_notice)).thenReturn("Visual")

        handler.setHtmlModeOn(true)
        handler.toggleHtmlMode()

        verify(analyticsSession).switchEditor(PostEditorAnalyticsSession.Editor.CLASSIC)
    }
    // endregion

    // region start tests
    @Test
    fun `handler works without listener before start`() {
        val newHandler = EditorModeHandler(dispatcher)
        // Should not throw
        newHandler.toggleHtmlMode()
        assertThat(newHandler.isHtmlModeOn()).isTrue()
    }
    // endregion
}
