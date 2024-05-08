package org.wordpress.android.models

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class NoteExtensionsTest : BaseUnitTest() {
    @Test
    fun `A comment_like note is not an achievement note`(){
        val note = mock<Note>()
        whenever(note.rawType).thenReturn("comment_like")
        val result = note.isAchievement()
        assertFalse(result)
    }

    @Test
    fun `A user_goal_met note is an achievement note`(){
        val note = mock<Note>()
        whenever(note.rawType).thenReturn("user_goal_met")
        val result = note.isAchievement()
        assertTrue(result)
    }
}
