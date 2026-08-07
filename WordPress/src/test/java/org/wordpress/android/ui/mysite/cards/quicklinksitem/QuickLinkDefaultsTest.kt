package org.wordpress.android.ui.mysite.cards.quicklinksitem

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.mysite.cards.quicklinksitem.QuickLinkDefaults.sortedByQuickLinkOrder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction

class QuickLinkDefaultsTest {
    @Test
    fun `default shortcuts lead in the declared order`() {
        val builderOrder = listOf(
            ListItemAction.POSTS,
            ListItemAction.PAGES,
            ListItemAction.MEDIA,
            ListItemAction.COMMENTS,
            ListItemAction.STATS,
            ListItemAction.SUBSCRIBERS
        )

        assertThat(builderOrder.sortedByQuickLinkOrder { it }).containsExactly(
            ListItemAction.STATS,
            ListItemAction.POSTS,
            ListItemAction.PAGES,
            ListItemAction.MEDIA,
            ListItemAction.COMMENTS,
            ListItemAction.SUBSCRIBERS
        )
    }

    @Test
    fun `non default shortcuts keep their relative order`() {
        val builderOrder = listOf(
            ListItemAction.COMMENTS,
            ListItemAction.STATS,
            ListItemAction.SUBSCRIBERS,
            ListItemAction.BLAZE,
            ListItemAction.ACTIVITY_LOG
        )

        assertThat(builderOrder.sortedByQuickLinkOrder { it }).containsExactly(
            ListItemAction.STATS,
            ListItemAction.COMMENTS,
            ListItemAction.SUBSCRIBERS,
            ListItemAction.BLAZE,
            ListItemAction.ACTIVITY_LOG
        )
    }

    @Test
    fun `a missing default shortcut does not disturb the rest`() {
        val builderOrder = listOf(
            ListItemAction.POSTS,
            ListItemAction.COMMENTS,
            ListItemAction.STATS
        )

        assertThat(builderOrder.sortedByQuickLinkOrder { it }).containsExactly(
            ListItemAction.STATS,
            ListItemAction.POSTS,
            ListItemAction.COMMENTS
        )
    }
}
