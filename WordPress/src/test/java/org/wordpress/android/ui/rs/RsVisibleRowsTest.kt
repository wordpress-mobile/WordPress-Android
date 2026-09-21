package org.wordpress.android.ui.rs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class RsVisibleRowsTest {
    private lateinit var rows: RsVisibleRows<String>

    @Before
    fun setUp() {
        rows = RsVisibleRows()
    }

    @Test
    fun `a tab nothing was recorded for is empty`() {
        assertThat(rows.visible(TAB)).isEmpty()
    }

    @Test
    fun `recording replaces the tab's rows rather than adding to them`() {
        rows.record(TAB, listOf(1L, 2L))
        rows.record(TAB, listOf(3L))

        assertThat(rows.visible(TAB)).containsExactly(3L)
    }

    /**
     * The bug this type exists for: the pager composes the neighbouring tab during a drag and its
     * visible-row stream reports against that tab. Held as one set, the neighbour would overwrite
     * the active tab's ids and its queued fetches would re-check the wrong set and drop.
     */
    @Test
    fun `a neighbouring tab does not overwrite another tab's rows`() {
        rows.record(TAB, listOf(1L, 2L))
        rows.record(OTHER_TAB, listOf(99L))

        assertThat(rows.visible(TAB)).containsExactlyInAnyOrder(1L, 2L)
        assertThat(rows.visible(OTHER_TAB)).containsExactly(99L)
    }

    @Test
    fun `clear forgets every tab`() {
        rows.record(TAB, listOf(1L))
        rows.record(OTHER_TAB, listOf(2L))

        rows.clear()

        assertThat(rows.visible(TAB)).isEmpty()
        assertThat(rows.visible(OTHER_TAB)).isEmpty()
    }

    companion object {
        private const val TAB = "published"
        private const val OTHER_TAB = "drafts"
    }
}
