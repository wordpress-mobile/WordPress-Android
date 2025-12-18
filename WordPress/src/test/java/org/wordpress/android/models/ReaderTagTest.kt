package org.wordpress.android.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ReaderTagTest {
    @Test
    fun `GIVEN tag with freshly pressed endpoint WHEN isFreshlyPressed THEN returns true`() {
        val tag = ReaderTag(
            ReaderTag.TAG_SLUG_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.FRESHLY_PRESSED_PATH,
            ReaderTagType.DEFAULT
        )

        assertThat(tag.isFreshlyPressed).isTrue()
    }

    @Test
    fun `GIVEN tag with different endpoint WHEN isFreshlyPressed THEN returns false`() {
        val tag = ReaderTag(
            "other",
            "Other",
            "Other",
            "/some/other/endpoint",
            ReaderTagType.DEFAULT
        )

        assertThat(tag.isFreshlyPressed).isFalse()
    }

    @Test
    fun `GIVEN tag with freshly pressed endpoint but wrong type WHEN isFreshlyPressed THEN returns false`() {
        val tag = ReaderTag(
            ReaderTag.TAG_SLUG_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.FRESHLY_PRESSED_PATH,
            ReaderTagType.FOLLOWED
        )

        assertThat(tag.isFreshlyPressed).isFalse()
    }

    @Test
    fun `GIVEN discover tag WHEN isFreshlyPressed THEN returns false`() {
        val tag = ReaderTag(
            "discover",
            "Discover",
            "Discover",
            ReaderTag.DISCOVER_PATH,
            ReaderTagType.DEFAULT
        )

        assertThat(tag.isFreshlyPressed).isFalse()
    }

    @Test
    fun `GIVEN discover tag WHEN isDiscover THEN returns true`() {
        val tag = ReaderTag(
            "discover",
            "Discover",
            "Discover",
            ReaderTag.DISCOVER_PATH,
            ReaderTagType.DEFAULT
        )

        assertThat(tag.isDiscover).isTrue()
    }

    @Test
    fun `GIVEN freshly pressed tag WHEN isDiscover THEN returns false`() {
        val tag = ReaderTag(
            ReaderTag.TAG_SLUG_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.TAG_TITLE_FRESHLY_PRESSED,
            ReaderTag.FRESHLY_PRESSED_PATH,
            ReaderTagType.DEFAULT
        )

        assertThat(tag.isDiscover).isFalse()
    }
}
