package org.wordpress.android.ui.reader.views.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ReaderTopBarScrollConnectionTest {
    private var canScroll = true
    private val connection = ReaderTopBarScrollConnection(canScroll = { canScroll }).apply {
        topBarHeight = TOP_BAR_HEIGHT
    }

    @Test
    fun `GIVEN bar shown WHEN content scrolls down THEN bar hides and consumes the scroll`() {
        val consumed = scroll(-40f)

        assertThat(connection.offset).isEqualTo(-40f)
        assertThat(consumed).isEqualTo(-40f)
    }

    @Test
    fun `GIVEN bar shown WHEN content scrolls past the bar height THEN bar stops at fully hidden`() {
        val consumed = scroll(-500f)

        assertThat(connection.offset).isEqualTo(-TOP_BAR_HEIGHT)
        assertThat(consumed).isEqualTo(-TOP_BAR_HEIGHT)
    }

    @Test
    fun `GIVEN bar hidden WHEN content scrolls down THEN nothing is consumed`() {
        scroll(-500f)

        assertThat(scroll(-30f)).isEqualTo(0f)
    }

    @Test
    fun `GIVEN bar hidden WHEN content scrolls up THEN bar shows again before the content scrolls`() {
        scroll(-500f)

        val consumed = scroll(30f)

        assertThat(connection.offset).isEqualTo(-TOP_BAR_HEIGHT + 30f)
        assertThat(consumed).isEqualTo(30f)
    }

    @Test
    fun `GIVEN bar shown WHEN content scrolls up THEN nothing is consumed`() {
        assertThat(scroll(30f)).isEqualTo(0f)
        assertThat(connection.offset).isEqualTo(0f)
    }

    @Test
    fun `GIVEN content can't scroll WHEN dragged THEN bar stays shown`() {
        canScroll = false

        assertThat(scroll(-40f)).isEqualTo(0f)
        assertThat(connection.offset).isEqualTo(0f)
    }

    @Test
    fun `GIVEN bar partly hidden and content can't scroll WHEN dragged up THEN bar can still show`() {
        scroll(-40f)
        canScroll = false

        scroll(40f)

        assertThat(connection.offset).isEqualTo(0f)
    }

    @Test
    fun `GIVEN bar hidden WHEN reset THEN bar shows`() {
        scroll(-500f)

        connection.reset()

        assertThat(connection.offset).isEqualTo(0f)
    }

    private fun scroll(deltaY: Float): Float =
        connection.onPreScroll(Offset(0f, deltaY), NestedScrollSource.UserInput).y

    companion object {
        private const val TOP_BAR_HEIGHT = 137f
    }
}
