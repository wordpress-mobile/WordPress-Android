package org.wordpress.android.ui.posts

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.fluxc.model.post.PostType.TypePage
import org.wordpress.android.fluxc.model.post.PostType.TypePortfolio
import org.wordpress.android.fluxc.model.post.PostType.TypePost

class PostTypeUtilsTest {
    @Test
    fun testPageTypeReturnPageValue() {
        assertEquals("page", getValueForType(TypePage, "page", "post"))
    }

    @Test
    fun testPostTypeReturnPostValue() {
        assertEquals("post", getValueForType(TypePost, "page", "post"))
    }

    @Test(expected = IllegalStateException::class)
    fun testPortfolioTypeThrows() {
        getValueForType(TypePortfolio, "page", "post")
    }
}
