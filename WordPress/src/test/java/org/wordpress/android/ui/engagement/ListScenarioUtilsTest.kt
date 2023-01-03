package org.wordpress.android.ui.engagement

import android.content.Context
import android.text.SpannableStringBuilder
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.models.Note
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_POST_LIKES
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.image.ImageManager

@RunWith(MockitoJUnitRunner::class)
class ListScenarioUtilsTest {
    @Mock
    lateinit var imageManager: ImageManager

    @Mock
    lateinit var notificationsUtilsWrapper: NotificationsUtilsWrapper

    @Mock
    lateinit var note: Note

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var spannableBuilder: SpannableStringBuilder

    private lateinit var listScenarioUtils: ListScenarioUtils

    private val siteId = 100L
    private val postId = 1000L

    @Before
    fun setup() {
        whenever(note.isLikeType).thenReturn(true)
        whenever(note.isPostLikeType).thenReturn(true)
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(note.postId).thenReturn(postId.toInt())
        whenever(note.siteId).thenReturn(siteId.toInt())
        whenever(
            spannableBuilder.getSpans(anyInt(), anyInt(), eq(NoteBlockClickableSpan::class.java))
        ).thenReturn(listOf<NoteBlockClickableSpan>().toTypedArray())
        whenever(spannableBuilder.length).thenReturn(0)
        whenever(
            notificationsUtilsWrapper.getSpannableContentForRanges(anyOrNull<FormattableContent>())
        ).thenReturn(spannableBuilder)

        listScenarioUtils = ListScenarioUtils(imageManager, notificationsUtilsWrapper)
    }

    @Test
    fun `exception is thrown if note is not like type`() {
        whenever(note.isLikeType).thenReturn(false)

        assertThrows(IllegalArgumentException::class.java) {
            listScenarioUtils.mapLikeNoteToListScenario(note, context)
        }
    }

    @Test
    fun `note is mapped to list scenario`() {
        val listScenario = listScenarioUtils.mapLikeNoteToListScenario(note, context)

        with(listScenario) {
            assertThat(type).isEqualTo(LOAD_POST_LIKES)
            assertThat(source).isEqualTo(EngagementNavigationSource.LIKE_NOTIFICATION_LIST)
            assertThat(siteId).isEqualTo(this@ListScenarioUtilsTest.siteId)
            assertThat(postId).isEqualTo(this@ListScenarioUtilsTest.postId)
            assertThat(commentPostId).isEqualTo(0L)
            assertThat(commentSiteUrl).isEqualTo("")
        }
    }

    @Test
    fun `json array is mapped to list of formattable content`() {
        val headerArray = mock<JSONArray>()
        val content = mock<FormattableContent>()
        val jsonObject = mock<JSONObject>()
        whenever(headerArray.length()).thenReturn(1)
        whenever(headerArray.getJSONObject(anyInt())).thenReturn(jsonObject)
        whenever(notificationsUtilsWrapper.mapJsonToFormattableContent(anyOrNull())).thenReturn(content)
        val formattableList = listScenarioUtils.transformToFormattableContentList(headerArray)

        assertThat(formattableList).isEqualTo(listOf(content))
    }
}
