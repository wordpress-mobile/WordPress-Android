package org.wordpress.android.ui.newstats.mostviewed

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsRowActionTest {
    @Test
    fun `given children, when the row is tapped, then it expands`() {
        val action = statsRowAction(hasChildren = true, url = null, hasItemClick = false)

        assertThat(action).isEqualTo(StatsRowAction.Expand)
    }

    @Test
    fun `given children and a url, then it still expands rather than navigating`() {
        val action = statsRowAction(hasChildren = true, url = URL, hasItemClick = false)

        assertThat(action).isEqualTo(StatsRowAction.Expand)
    }

    @Test
    fun `given children and an item click, then it still expands`() {
        val action = statsRowAction(hasChildren = true, url = null, hasItemClick = true)

        assertThat(action).isEqualTo(StatsRowAction.Expand)
    }

    @Test
    fun `given no children and a url, then it opens the url`() {
        val action = statsRowAction(hasChildren = false, url = URL, hasItemClick = false)

        assertThat(action).isEqualTo(StatsRowAction.OpenUrl(URL))
    }

    /**
     * Posts carry a url *and* an item click. The item click has to win, or a post row would open
     * the post in a browser instead of its detail stats.
     */
    @Test
    fun `given both a url and an item click, then the item click wins`() {
        val action = statsRowAction(hasChildren = false, url = URL, hasItemClick = true)

        assertThat(action).isEqualTo(StatsRowAction.Item)
    }

    @Test
    fun `given an item click and no url, then the item click is used`() {
        val action = statsRowAction(hasChildren = false, url = null, hasItemClick = true)

        assertThat(action).isEqualTo(StatsRowAction.Item)
    }

    @Test
    fun `given nothing to act on, then the row is inert`() {
        val action = statsRowAction(hasChildren = false, url = null, hasItemClick = false)

        assertThat(action).isEqualTo(StatsRowAction.None)
    }

    @Test
    fun `given a blank url, then the row is inert`() {
        val action = statsRowAction(hasChildren = false, url = "   ", hasItemClick = false)

        assertThat(action).isEqualTo(StatsRowAction.None)
    }

    @Test
    fun `given an empty url, then the row is inert`() {
        val action = statsRowAction(hasChildren = false, url = "", hasItemClick = false)

        assertThat(action).isEqualTo(StatsRowAction.None)
    }

    companion object {
        private const val URL = "https://example.com/"
    }
}
