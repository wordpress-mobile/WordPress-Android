package org.wordpress.android.push

import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.apache.commons.text.StringEscapeUtils
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.Note
import org.wordpress.android.push.GCMMessageHandler.PUSH_TYPE_COMMENT
import org.wordpress.android.push.GCMMessageHandler.PUSH_ARG_MSG
import org.wordpress.android.push.GCMMessageHandler.PUSH_ARG_TITLE
import org.wordpress.android.push.GCMMessageService.PUSH_ARG_NOTE_ID
import org.wordpress.android.ui.notifications.SystemNotificationsTracker
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import kotlin.test.assertEquals

private const val PUSH_TYPE_NOT_A_COMMENT = "NotAComment"

@ExperimentalCoroutinesApi
class NotificationHelperTest : BaseUnitTest() {
    private val systemNotificationsTracker: SystemNotificationsTracker = mock()
    private val gcmMessageHandler: GCMMessageHandler = mock()
    private val notificationsUtilsWrapper = mock<NotificationsUtilsWrapper>()
    private val notificationHelper =
        GCMMessageHandler.NotificationHelper(gcmMessageHandler, systemNotificationsTracker, notificationsUtilsWrapper)

    @Test
    fun `WHEN a PN that is a comment has a message argument THEN then the message is used as title`() {
        val expectedTitle = "expectedTitle"
        val defaultTitle = "defaultTitle"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_MSG)).thenReturn(StringEscapeUtils.escapeHtml4(expectedTitle))
        val title = notificationHelper.getNotificationTitle(mockedBundle, PUSH_TYPE_COMMENT, defaultTitle)
        assertEquals(expectedTitle, title)
    }

    @Test
    fun `WHEN a PN that is not a comment contains a title argument THEN this title is used`() {
        val expectedTitle = "expectedTitle"
        val defaultTitle = "defaultTitle"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_TITLE)).thenReturn(StringEscapeUtils.escapeHtml4(expectedTitle))
        val title = notificationHelper.getNotificationTitle(mockedBundle, PUSH_TYPE_NOT_A_COMMENT, defaultTitle)
        assertEquals(expectedTitle, title)
    }

    @Test
    fun `WHEN a PN that is not a comment does not contain a title argument THEN the default title is used`() {
        val defaultTitle = "defaultTitle"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_TITLE)).thenReturn(null)
        val title = notificationHelper.getNotificationTitle(mockedBundle, PUSH_TYPE_NOT_A_COMMENT, defaultTitle)
        assertEquals(defaultTitle, title)
    }

    @Test
    fun `WHEN a PN that is not a comment contains a message argument THEN the message is used`() {
        val expectedMessage = "expectedMessage"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_MSG)).thenReturn(StringEscapeUtils.escapeHtml4(expectedMessage))
        val message = notificationHelper.getNotificationMessage(mockedBundle, PUSH_TYPE_NOT_A_COMMENT)
        assertEquals(expectedMessage, message)
    }

    @Test
    fun `WHEN a PN that is not a comment does not contain a message argument THEN an empty message is used`() {
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_MSG)).thenReturn(null)
        val message = notificationHelper.getNotificationMessage(mockedBundle, PUSH_TYPE_NOT_A_COMMENT)
        assertEquals("", message)
    }

    @Test
    fun `WHEN a PN that is a comment has a comment payload THEN the comment is used as a message`() {
        val expectedMessage = "expectedMessage"
        val mockedNote = mock<Note>()
        val noteId = "noteId"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_NOTE_ID)).thenReturn(noteId)
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(mockedNote)
        whenever(mockedNote.commentSubject).thenReturn(expectedMessage)
        val message = notificationHelper.getNotificationMessage(mockedBundle, PUSH_TYPE_COMMENT)
        assertEquals(expectedMessage, message)
    }

    @Test
    fun `WHEN a PN that is a comment does not have a comment payload and contains a message THEN the later is used`() {
        val expectedMessage = "expectedMessage"
        val mockedNote = mock<Note>()
        val noteId = "noteId"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_NOTE_ID)).thenReturn(noteId)
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(mockedNote)
        whenever(mockedNote.commentSubject).thenReturn(null)
        whenever(mockedBundle.getString(PUSH_ARG_MSG)).thenReturn(StringEscapeUtils.escapeHtml4(expectedMessage))
        val message = notificationHelper.getNotificationMessage(mockedBundle, PUSH_TYPE_COMMENT)
        assertEquals(expectedMessage, message)
    }

    @Test
    fun `WHEN a PN that is a comment does not have a comment payload or a message THEN an empty message is used`() {
        val mockedNote = mock<Note>()
        val noteId = "noteId"
        val mockedBundle = mock<Bundle>()
        whenever(mockedBundle.getString(PUSH_ARG_NOTE_ID)).thenReturn(noteId)
        whenever(notificationsUtilsWrapper.getNoteById(noteId)).thenReturn(mockedNote)
        whenever(mockedNote.commentSubject).thenReturn(null)
        whenever(mockedBundle.getString(PUSH_ARG_MSG)).thenReturn(null)
        val message = notificationHelper.getNotificationMessage(mockedBundle, PUSH_TYPE_COMMENT)
        assertEquals("", message)
    }
}
