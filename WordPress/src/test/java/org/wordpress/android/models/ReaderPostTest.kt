package org.wordpress.android.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.lang.reflect.Method

/**
 * Tests for ReaderPost, specifically the getEditorialImage logic for Freshly Pressed posts.
 *
 * Note: Testing fromJson() directly requires complex setup due to static dependencies.
 * These tests focus on the mshots filtering logic using reflection to access the private method.
 */
class ReaderPostTest {
    @Test
    fun `GIVEN mshots image url WHEN getEditorialImage THEN returns null`() {
        val mshotsUrl = "https://s0.wp.com/mshots/v1/https://example.com"

        val result = invokeGetEditorialImage(mshotsUrl)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN valid image url WHEN getEditorialImage THEN returns url`() {
        val validUrl = "https://example.com/image.jpg"

        val result = invokeGetEditorialImage(validUrl)

        assertThat(result).isEqualTo(validUrl)
    }

    @Test
    fun `GIVEN null image url WHEN getEditorialImage THEN returns null`() {
        val result = invokeGetEditorialImage(null)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN wp_com mshots url WHEN getEditorialImage THEN returns null`() {
        val mshotsUrl = "https://i0.wp.com/mshots/screenshot.png"

        val result = invokeGetEditorialImage(mshotsUrl)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN url without mshots WHEN getEditorialImage THEN returns url`() {
        val normalUrl = "https://wordpress.com/some-image.jpg"

        val result = invokeGetEditorialImage(normalUrl)

        assertThat(result).isEqualTo(normalUrl)
    }

    /**
     * Uses reflection to invoke the private getEditorialImage method.
     */
    private fun invokeGetEditorialImage(imageUrl: String?): String? {
        val method: Method = ReaderPost::class.java.getDeclaredMethod(
            "getEditorialImage",
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(null, imageUrl) as String?
    }
}
