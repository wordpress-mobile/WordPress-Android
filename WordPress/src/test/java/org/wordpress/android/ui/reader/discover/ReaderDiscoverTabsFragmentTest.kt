package org.wordpress.android.ui.reader.discover

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.reader.discover.ReaderDiscoverTabsFragment.Companion.resolveInitialTabIndex

class ReaderDiscoverTabsFragmentTest {
    @Test
    fun `GIVEN stored index within range WHEN resolveInitialTabIndex THEN returns stored index`() {
        assertThat(resolveInitialTabIndex(storedIndex = 1, tabCount = 3)).isEqualTo(1)
    }

    @Test
    fun `GIVEN stored index equals last tab WHEN resolveInitialTabIndex THEN returns last tab`() {
        assertThat(resolveInitialTabIndex(storedIndex = 2, tabCount = 3)).isEqualTo(2)
    }

    @Test
    fun `GIVEN stored index beyond range WHEN resolveInitialTabIndex THEN clamps to last tab`() {
        assertThat(resolveInitialTabIndex(storedIndex = 99, tabCount = 3)).isEqualTo(2)
    }

    @Test
    fun `GIVEN negative stored index WHEN resolveInitialTabIndex THEN clamps to 0`() {
        assertThat(resolveInitialTabIndex(storedIndex = -1, tabCount = 3)).isEqualTo(0)
    }

    @Test
    fun `GIVEN empty tab list WHEN resolveInitialTabIndex THEN returns 0`() {
        assertThat(resolveInitialTabIndex(storedIndex = 0, tabCount = 0)).isEqualTo(0)
        assertThat(resolveInitialTabIndex(storedIndex = 5, tabCount = 0)).isEqualTo(0)
    }
}
