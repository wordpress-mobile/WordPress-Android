package org.wordpress.android.ui.postsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PostRsTabLoadingTest {
    @Test
    fun `refresh starts loading when nothing is cached and nothing was fetched`() {
        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = false)).isTrue()
    }

    @Test
    fun `refresh does not start loading when a tab is known to be empty`() {
        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = true)).isFalse()
    }

    @Test
    fun `refresh does not start loading over cached posts`() {
        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = true, hasFetched = false)).isFalse()
        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = true, hasFetched = true)).isFalse()
    }

    @Test
    fun `an empty cache read mid-fetch keeps the placeholders`() {
        assertThat(PostRsTabLoading.onItemsLoaded(wasLoading = true, hasItems = false)).isTrue()
    }

    @Test
    fun `loading ends once there are items to show`() {
        assertThat(PostRsTabLoading.onItemsLoaded(wasLoading = true, hasItems = true)).isFalse()
    }

    @Test
    fun `loading ends when the first page is no longer being fetched`() {
        assertThat(
            PostRsTabLoading.onListInfoChanged(wasLoading = true, isFetchingFirstPage = false)
        ).isFalse()
    }

    @Test
    fun `loading continues while the first page is being fetched`() {
        assertThat(
            PostRsTabLoading.onListInfoChanged(wasLoading = true, isFetchingFirstPage = true)
        ).isTrue()
    }

    @Test
    fun `nothing resurrects loading once it has ended`() {
        assertThat(PostRsTabLoading.onItemsLoaded(wasLoading = false, hasItems = false)).isFalse()
        assertThat(
            PostRsTabLoading.onListInfoChanged(wasLoading = false, isFetchingFirstPage = true)
        ).isFalse()
    }

    @Test
    fun `first visit shows placeholders until the fetch delivers posts`() {
        // Nothing cached, no fetch behind us: the refresh puts the tab into loading.
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
    fun `first visit to an empty site ends loading when the fetch resolves`() {
        var isLoading = PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = false)
        isLoading = PostRsTabLoading.onListInfoChanged(
            wasLoading = isLoading,
            isFetchingFirstPage = true
        )
        assertThat(isLoading).isTrue()

        isLoading = PostRsTabLoading.onListInfoChanged(
            wasLoading = isLoading,
            isFetchingFirstPage = false
        )
        assertThat(isLoading).isFalse()

        // A later refresh of the now known-empty tab keeps the empty message.
        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = true)).isFalse()
    }
}
