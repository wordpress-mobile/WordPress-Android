package org.wordpress.android.ui.rs.contentlist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Values shared by the rs content list view models and screens. */
object ContentListDefaults {
    const val SEARCH_DEBOUNCE_MS = 250L

    const val MIN_SEARCH_QUERY_LENGTH = 3

    /**
     * Bottom padding that lets the last row scroll clear of the extended FAB (56dp plus its 16dp
     * margin), which would otherwise sit over that row's footer buttons and overflow menu.
     */
    val FabClearancePadding = PaddingValues(bottom = 88.dp)

    /**
     * The page colour behind the redesigned cards, one step recessed from `surface`. Dark mode needs
     * a different role because its `surface` is darker than `surfaceContainerLow`.
     */
    @Composable
    fun containerColor(): Color = when {
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
}
