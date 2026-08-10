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
        isLoading = PostRsTabLoading.onListInfoChanged(isLoading, isFetchingFirstPage = true)
        assertThat(isLoading).isTrue()

        isLoading = PostRsTabLoading.onListInfoChanged(isLoading, isFetchingFirstPage = false)
        assertThat(isLoading).isFalse()

        assertThat(PostRsTabLoading.onRefreshStarted(hasPosts = false, hasFetched = true)).isFalse()
    }

    @Test
    fun `loading never resurrects once it has ended`() {
        assertThat(PostRsTabLoading.onItemsLoaded(wasLoading = false, hasItems = false)).isFalse()
        assertThat(PostRsTabLoading.onListInfoChanged(false, isFetchingFirstPage = true)).isFalse()
    }
}
