package org.wordpress.android.ui.rs.contentlist

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Tests for [ContentListDensity]'s mapping to and from the stored boolean preference, which is what
 * the list reads on every open.
 */
class ContentListDensityTest {
    @Test
    fun `a stored true is condensed`() {
        assertThat(ContentListDensity.of(true)).isEqualTo(ContentListDensity.CONDENSED)
    }

    @Test
    fun `a stored false is comfortable`() {
        assertThat(ContentListDensity.of(false)).isEqualTo(ContentListDensity.COMFORTABLE)
    }

    @Test
    fun `only condensed reports itself as condensed`() {
        assertThat(ContentListDensity.CONDENSED.isCondensed).isTrue()
        assertThat(ContentListDensity.COMFORTABLE.isCondensed).isFalse()
    }

    @Test
    fun `the flag round-trips through of`() {
        ContentListDensity.entries.forEach { density ->
            assertThat(ContentListDensity.of(density.isCondensed)).isEqualTo(density)
        }
    }
}
