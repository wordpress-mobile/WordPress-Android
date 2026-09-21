package org.wordpress.android.ui.rs

/**
 * One tab of a list backed by an rs observable collection.
 *
 * Generic in the only dimension the screens actually differ in - what a row is. Posts hold their
 * model directly, pages wrap theirs to carry hierarchy and the synthetic Homepage / Posts page /
 * Site Editor rows, and comments hold their own model. The other seven fields are the same
 * everywhere, which is what makes this worth sharing rather than three coincidentally-alike types.
 *
 * Deliberately has no `isEmpty`: "this tab has nothing in it" is not the same question on every
 * screen. A block-theme site gets the Site Editor row prepended even with no pages at all, so the
 * pages list asks `hasRealPages` instead - a shared convenience property here would invite the two
 * to be collapsed into one wrong answer.
 */
data class RsTabUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val isAuthError: Boolean = false
)
