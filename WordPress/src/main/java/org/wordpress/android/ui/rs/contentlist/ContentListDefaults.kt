package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** What the rs content list view models and screens both read: search timing and the page colour. */
object ContentListDefaults {
    /** Search waits this long after the last keystroke, so a word being typed is one request. */
    const val SEARCH_DEBOUNCE_MS = 250L

    /** Shorter queries match too much to be useful, so the lists don't search until this length. */
    const val MIN_SEARCH_QUERY_LENGTH = 3

    /**
     * The colour behind the list. Redesigned cards are drawn on `surface`, so the page behind them
     * has to sit one step recessed or they read as a flat sheet. Which role that is differs by mode:
     * this app's dark scheme makes `surface` darker than `surfaceContainerLow`, so reusing the
     * light-mode role there would put the page *above* the cards. The pre-redesign list keeps the
     * theme background.
     */
    @Composable
    fun containerColor(isRedesignEnabled: Boolean): Color = when {
        !isRedesignEnabled -> MaterialTheme.colorScheme.background
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
}
