package org.wordpress.android.ui.newstats.mostviewed

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StatsRowActionTest {
    @Test
    fun `given children, when the row is tapped, then it expands`() {
        val action = statsRowAction(hasChildren = true, url = null, onItemClick = null)

        assertThat(action).isEqualTo(StatsRowAction.Expand)
    }

    @Test
    fun `given children and a url, then it still expands rather than navigating`() {
        val action = statsRowAction(hasChildren = true, url = URL, onItemClick = null)

        assertThat(action).isEqualTo(StatsRowAction.Expand)
    }

    @Test
    fun `given children and an item click, then it still expands`() {
        val action = statsRowAction(hasChildren = true, url = null, onItemClick = {})

        assertThat(action).isEqualTo(StatsRowAction.Expand)
    }

    @Test
    fun `given no children and a url, then it opens the url`() {
        val action = statsRowAction(hasChildren = false, url = URL, onItemClick = null)

        assertThat(action).isEqualTo(StatsRowAction.OpenUrl(URL))
    }

    /**
     * Posts carry a url *and* an item click. The item click has to win, or a post row would open
     * the post in a browser instead of its detail stats.
     */
    @Test
    fun `given both a url and an item click, then the item click wins`() {
        var invoked = false

        val action = statsRowAction(hasChildren = false, url = URL, onItemClick = { invoked = true })

        assertThat(action).isInstanceOf(StatsRowAction.Item::class.java)
        (action as StatsRowAction.Item).onClick()
        assertThat(invoked).isTrue()
    }

    @Test
    fun `given an item click and no url, then the item click is carried through`() {
        var invoked = false

        val action = statsRowAction(hasChildren = false, url = null, onItemClick = { invoked = true })

        assertThat(action).isInstanceOf(StatsRowAction.Item::class.java)
        (action as StatsRowAction.Item).onClick()
        assertThat(invoked).isTrue()
    }

    @Test
    fun `given nothing to act on, then the row is inert`() {
        val action = statsRowAction(hasChildren = false, url = null, onItemClick = null)

        assertThat(action).isEqualTo(StatsRowAction.None)
    }

    @Test
    fun `given a blank url, then the row is inert`() {
        val action = statsRowAction(hasChildren = false, url = "   ", onItemClick = null)

        assertThat(action).isEqualTo(StatsRowAction.None)
    }

    @Test
    fun `given an empty url, then the row is inert`() {
        val action = statsRowAction(hasChildren = false, url = "", onItemClick = null)

        assertThat(action).isEqualTo(StatsRowAction.None)
    }

    companion object {
        private const val URL = "https://example.com/"
    }
}
