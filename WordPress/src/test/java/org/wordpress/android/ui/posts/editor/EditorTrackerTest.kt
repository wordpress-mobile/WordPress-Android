package org.wordpress.android.ui.posts.editor

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.editor.EditorFragmentAbstract.TrackableEvent
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class EditorTrackerTest {
    @Test
    fun `verify trackEditorEvent maps TrackableEvent to correct Stat item`() {
        // Arrange
        val analyticsTracker = mock<AnalyticsTrackerWrapper>()
        val editorTracker = createEditorTracker(analyticsTracker)
        // Act
        trackEventToStatMap.forEach { (trackableEvent, expectedStat) ->
            editorTracker.trackEditorEvent(trackableEvent, "")
            // Assert
            verify(analyticsTracker).track(eq(expectedStat), anyOrNull<Map<String, *>>())
        }
    }

    @Test
    fun `verify trackEditorEvent adds correct editor name property to all events`() {
        // Arrange
        val analyticsTracker = mock<AnalyticsTrackerWrapper>()
        val editorTracker = createEditorTracker(analyticsTracker)
        // Act
        trackEventToStatMap.forEach { (trackableEvent, _) ->
            editorTracker.trackEditorEvent(trackableEvent, editorName)
        }
        // Assert
        verify(analyticsTracker, times(trackEventToStatMap.size))
                .track(anyOrNull(), eq(mapOf("editor" to editorName)))
    }

    private companion object Fixtures {
        private const val editorName = "test_editor_name"
        private val trackEventToStatMap = mapOf(
                TrackableEvent.BOLD_BUTTON_TAPPED to Stat.EDITOR_TAPPED_BOLD,
                TrackableEvent.BLOCKQUOTE_BUTTON_TAPPED to Stat.EDITOR_TAPPED_BLOCKQUOTE,
                TrackableEvent.ELLIPSIS_COLLAPSE_BUTTON_TAPPED to Stat.EDITOR_TAPPED_ELLIPSIS_COLLAPSE,
                TrackableEvent.ELLIPSIS_EXPAND_BUTTON_TAPPED to Stat.EDITOR_TAPPED_ELLIPSIS_EXPAND,
                TrackableEvent.HEADING_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING,
                TrackableEvent.HEADING_1_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING_1,
                TrackableEvent.HEADING_2_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING_2,
                TrackableEvent.HEADING_3_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING_3,
                TrackableEvent.HEADING_4_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING_4,
                TrackableEvent.HEADING_5_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING_5,
                TrackableEvent.HEADING_6_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HEADING_6,
                TrackableEvent.HORIZONTAL_RULE_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HORIZONTAL_RULE,
                TrackableEvent.FORMAT_ALIGN_LEFT_BUTTON_TAPPED to Stat.EDITOR_TAPPED_ALIGN_LEFT,
                TrackableEvent.FORMAT_ALIGN_CENTER_BUTTON_TAPPED to Stat.EDITOR_TAPPED_ALIGN_CENTER,
                TrackableEvent.FORMAT_ALIGN_RIGHT_BUTTON_TAPPED to Stat.EDITOR_TAPPED_ALIGN_RIGHT,
                TrackableEvent.HTML_BUTTON_TAPPED to Stat.EDITOR_TAPPED_HTML,
                TrackableEvent.IMAGE_EDITED to Stat.EDITOR_EDITED_IMAGE,
                TrackableEvent.ITALIC_BUTTON_TAPPED to Stat.EDITOR_TAPPED_ITALIC,
                TrackableEvent.LINK_ADDED_BUTTON_TAPPED to Stat.EDITOR_TAPPED_LINK_ADDED,
                TrackableEvent.LIST_BUTTON_TAPPED to Stat.EDITOR_TAPPED_LIST,
                TrackableEvent.LIST_ORDERED_BUTTON_TAPPED to Stat.EDITOR_TAPPED_LIST_ORDERED,
                TrackableEvent.LIST_UNORDERED_BUTTON_TAPPED to Stat.EDITOR_TAPPED_LIST_UNORDERED,
                TrackableEvent.MEDIA_BUTTON_TAPPED to Stat.EDITOR_TAPPED_IMAGE,
                TrackableEvent.NEXT_PAGE_BUTTON_TAPPED to Stat.EDITOR_TAPPED_NEXT_PAGE,
                TrackableEvent.PARAGRAPH_BUTTON_TAPPED to Stat.EDITOR_TAPPED_PARAGRAPH,
                TrackableEvent.PREFORMAT_BUTTON_TAPPED to Stat.EDITOR_TAPPED_PREFORMAT,
                TrackableEvent.READ_MORE_BUTTON_TAPPED to Stat.EDITOR_TAPPED_READ_MORE,
                TrackableEvent.STRIKETHROUGH_BUTTON_TAPPED to Stat.EDITOR_TAPPED_STRIKETHROUGH,
                TrackableEvent.UNDERLINE_BUTTON_TAPPED to Stat.EDITOR_TAPPED_UNDERLINE,
                TrackableEvent.REDO_TAPPED to Stat.EDITOR_TAPPED_REDO,
                TrackableEvent.UNDO_TAPPED to Stat.EDITOR_TAPPED_UNDO
        )

        fun createEditorTracker(analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()) =
                EditorTracker(mock(), analyticsTrackerWrapper)
    }
}
