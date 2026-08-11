package org.wordpress.android.ui.postsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PostRsTabLoadingTest {
    @Test
    fun `first visit shows placeholders until the fetch delivers posts`() {
        // Nothing cached and no fetch behind us, so the refresh puts the tab into loading.
        var isLoading = PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = false)
        assertThat(isLoading).isTrue()

        // The cache is still empty while the fetch runs, so the placeholders stay up.
        isLoading = PostRsTabLoading.onItemsLoaded(wasLoading = isLoading, hasItems = false)
        assertThat(isLoading).isTrue()

        // The fetch lands and the posts replace the placeholders.
        isLoading = PostRsTabLoading.onItemsLoaded(wasLoading = isLoading, hasItems = true)
        assertThat(isLoading).isFalse()
    }

    @Test
    fun `an empty site stops loading once fetched and stays empty on later refreshes`() {
        var isLoading = PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = false)
        isLoading = PostRsTabLoading.onListInfoChanged(
            wasLoading = isLoading,
            isFetchingFirstPage = true,
            hasPosts = false,
            hasFetched = false
        )
        assertThat(isLoading).isTrue()

        isLoading = PostRsTabLoading.onListInfoChanged(
            wasLoading = isLoading,
            isFetchingFirstPage = false,
            hasPosts = false,
            hasFetched = true
        )
        assertThat(isLoading).isFalse()

        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = true)).isFalse()
    }

    @Test
    fun `list info resolving ahead of its data keeps the placeholders`() {
        // The collection has stopped fetching but the items haven't been read yet, so the tab
        // waits instead of flashing its empty message for a frame.
        assertThat(
            PostRsTabLoading.onListInfoChanged(
                wasLoading = true,
                isFetchingFirstPage = false,
                hasPosts = false,
                hasFetched = false
            )
        ).isTrue()
    }

    @Test
    fun `loading never resurrects once it has ended`() {
        assertThat(PostRsTabLoading.onItemsLoaded(wasLoading = false, hasItems = false)).isFalse()
        assertThat(
            PostRsTabLoading.onListInfoChanged(
                wasLoading = false,
                isFetchingFirstPage = true,
                hasPosts = false,
                hasFetched = false
            )
        ).isFalse()
    }
}
