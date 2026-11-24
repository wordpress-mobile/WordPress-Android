package org.wordpress.android.ui.newstats.mostviewed

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.R

class StatsPostThumbnailTest {
    @Test
    fun `when the item is a post, then the post glyph is used`() {
        assertThat(postTypeIconRes("post")).isEqualTo(R.drawable.ic_posts_white_24dp)
    }

    @Test
    fun `when the item type is capitalised, then it is still recognised`() {
        assertThat(postTypeIconRes("Post")).isEqualTo(R.drawable.ic_posts_white_24dp)
    }

    @Test
    fun `when the item is a page, the homepage or an attachment, then the page glyph is used`() {
        assertThat(postTypeIconRes("page")).isEqualTo(R.drawable.ic_pages_white_24dp)
        assertThat(postTypeIconRes("homepage")).isEqualTo(R.drawable.ic_pages_white_24dp)
        assertThat(postTypeIconRes("attachment")).isEqualTo(R.drawable.ic_pages_white_24dp)
    }

    @Test
    fun `when the item type is unknown or absent, then the page glyph is used`() {
        assertThat(postTypeIconRes("product")).isEqualTo(R.drawable.ic_pages_white_24dp)
        assertThat(postTypeIconRes(null)).isEqualTo(R.drawable.ic_pages_white_24dp)
        assertThat(postTypeIconRes("")).isEqualTo(R.drawable.ic_pages_white_24dp)
    }
}
