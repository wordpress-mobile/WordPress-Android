package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Values shared by the rs content list view models and screens. */
object ContentListDefaults {
    const val SEARCH_DEBOUNCE_MS = 250L

    const val MIN_SEARCH_QUERY_LENGTH = 3

    /**
     * The page colour behind the redesigned cards, one step recessed from `surface`. Dark mode needs
     * a different role because its `surface` is darker than `surfaceContainerLow`.
     */
    @Composable
    fun containerColor(isRedesignEnabled: Boolean): Color = when {
        !isRedesignEnabled -> MaterialTheme.colorScheme.background
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
}
