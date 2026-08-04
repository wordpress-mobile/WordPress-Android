package org.wordpress.android.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.stats.StatsConstants

class StatsDetailItemTypeTest {
    @Test
    fun `post type maps to post detail item type`() {
        assertThat(statsDetailItemType("post")).isEqualTo(StatsConstants.ITEM_TYPE_POST)
    }

    @Test
    fun `attachment type maps to attachment detail item type`() {
        assertThat(statsDetailItemType("attachment")).isEqualTo(StatsConstants.ITEM_TYPE_ATTACHMENT)
    }

    @Test
    fun `page, homepage, unknown and null types map to home page detail item type`() {
        assertThat(statsDetailItemType("page")).isEqualTo(StatsConstants.ITEM_TYPE_HOME_PAGE)
        assertThat(statsDetailItemType("homepage")).isEqualTo(StatsConstants.ITEM_TYPE_HOME_PAGE)
        assertThat(statsDetailItemType("other")).isEqualTo(StatsConstants.ITEM_TYPE_HOME_PAGE)
        assertThat(statsDetailItemType(null)).isEqualTo(StatsConstants.ITEM_TYPE_HOME_PAGE)
    }
}
