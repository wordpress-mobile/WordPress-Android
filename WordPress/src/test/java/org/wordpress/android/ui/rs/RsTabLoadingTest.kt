package org.wordpress.android.ui.rs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RsTabLoadingTest {
    @Test
    fun `first visit shows placeholders until the fetch delivers items`() {
        // Nothing cached and no fetch behind us, so the refresh puts the tab into loading.
        var isLoading = RsTabLoading.onRefreshStarted(hasItems = false, hasFetched = false)
        assertThat(isLoading).isTrue()

        // The cache is still empty while the fetch runs, so the placeholders stay up.
        isLoading = RsTabLoading.onItemsLoaded(wasLoading = isLoading, hasItems = false)
        assertThat(isLoading).isTrue()

        // The fetch lands and the items replace the placeholders.
        isLoading = RsTabLoading.onItemsLoaded(wasLoading = isLoading, hasItems = true)
        assertThat(isLoading).isFalse()
    }

    @Test
    fun `an empty site stops loading once fetched and stays empty on later refreshes`() {
        var isLoading = RsTabLoading.onRefreshStarted(hasItems = false, hasFetched = false)
        isLoading = RsTabLoading.onListInfoChanged(
            wasLoading = isLoading,
            isFetchingFirstPage = true,
            hasItems = false,
            hasFetched = false
        )
        assertThat(isLoading).isTrue()

        isLoading = RsTabLoading.onListInfoChanged(
            wasLoading = isLoading,
            isFetchingFirstPage = false,
            hasItems = false,
            hasFetched = true
        )
        assertThat(isLoading).isFalse()

        assertThat(RsTabLoading.onRefreshStarted(hasItems = false, hasFetched = true)).isFalse()
    }

    @Test
    fun `list info resolving ahead of its data keeps the placeholders`() {
        // The collection has stopped fetching but the items haven't been read yet, so the tab
        // waits instead of flashing its empty message for a frame.
        assertThat(
            RsTabLoading.onListInfoChanged(
                wasLoading = true,
                isFetchingFirstPage = false,
                hasItems = false,
                hasFetched = false
            )
        ).isTrue()
    }

    @Test
    fun `loading never resurrects once it has ended`() {
        assertThat(RsTabLoading.onItemsLoaded(wasLoading = false, hasItems = false)).isFalse()
        assertThat(
            RsTabLoading.onListInfoChanged(
                wasLoading = false,
                isFetchingFirstPage = true,
                hasItems = false,
                hasFetched = false
            )
        ).isFalse()
    }
}
