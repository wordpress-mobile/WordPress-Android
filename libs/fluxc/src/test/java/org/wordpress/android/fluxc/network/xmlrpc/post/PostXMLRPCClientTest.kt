package org.wordpress.android.fluxc.network.xmlrpc.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(RobolectricTestRunner::class)
class PostXMLRPCClientTest {
    private val site = SiteModel().apply { id = 1 }

    @Test
    fun `page slug is preserved when wp_slug is empty`() {
        val expectedSlug = "my-custom-slug"
        val postMap = createPageMap(postName = expectedSlug, wpSlug = "")

        val result = PostXMLRPCClient.postResponseObjectToPostModel(postMap, site)

        assertTrue(result.isPage)
        assertEquals(expectedSlug, result.slug)
    }

    @Test
    fun `page slug is preserved when wp_slug is missing`() {
        val expectedSlug = "my-custom-slug"
        val postMap = createPageMap(postName = expectedSlug, wpSlug = null)

        val result = PostXMLRPCClient.postResponseObjectToPostModel(postMap, site)

        assertTrue(result.isPage)
        assertEquals(expectedSlug, result.slug)
    }

    @Test
    fun `page slug uses wp_slug when wp_slug has value`() {
        val wpSlugValue = "wp-slug-value"
        val postMap = createPageMap(postName = "post-name-value", wpSlug = wpSlugValue)

        val result = PostXMLRPCClient.postResponseObjectToPostModel(postMap, site)

        assertTrue(result.isPage)
        assertEquals(wpSlugValue, result.slug)
    }

    @Test
    fun `post slug uses post_name correctly`() {
        val expectedSlug = "my-post-slug"
        val postMap = createPostMap(postName = expectedSlug)

        val result = PostXMLRPCClient.postResponseObjectToPostModel(postMap, site)

        assertFalse(result.isPage)
        assertEquals(expectedSlug, result.slug)
    }

    private fun createPageMap(postName: String, wpSlug: String?): Map<String, Any?> {
        return mutableMapOf<String, Any?>(
            "post_id" to "123",
            "post_title" to "Test Page",
            "post_date_gmt" to java.util.Date(),
            "post_modified_gmt" to java.util.Date(),
            "post_content" to "Page content",
            "link" to "https://example.com/page",
            "terms" to arrayOf<Any>(),
            "custom_fields" to arrayOf<Any>(),
            "post_excerpt" to "",
            "post_name" to postName,
            "post_password" to "",
            "post_status" to "publish",
            "post_type" to "page",
            "post_parent" to 0L,
            "wp_page_parent" to ""
        ).also {
            if (wpSlug != null) {
                it["wp_slug"] = wpSlug
            }
        }
    }

    private fun createPostMap(postName: String): Map<String, Any?> {
        return mapOf(
            "post_id" to "456",
            "post_title" to "Test Post",
            "post_date_gmt" to java.util.Date(),
            "post_modified_gmt" to java.util.Date(),
            "post_content" to "Post content",
            "link" to "https://example.com/post",
            "terms" to arrayOf<Any>(),
            "custom_fields" to arrayOf<Any>(),
            "post_excerpt" to "",
            "post_name" to postName,
            "post_password" to "",
            "post_status" to "publish",
            "post_type" to "post",
            "post_thumbnail" to emptyMap<String, Any>(),
            "post_format" to ""
        )
    }
}
