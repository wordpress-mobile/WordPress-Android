package org.wordpress.android.ui.rs.contentlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R

/**
 * Flips the list between its two densities. Deliberately a top-bar action rather than an overflow
 * item: a view control the user is expected to find has to be visible.
 */
@Composable
fun ContentListDensityToggle(
    density: ContentListDensity,
    onToggle: () -> Unit
) {
    val labelResId = if (density.isCondensed) {
        R.string.content_list_density_show_comfortable
    } else {
        R.string.content_list_density_show_condensed
    }
    IconButton(onClick = onToggle) {
        // Both glyphs come from the same family - bars at different spacing - so the two states
        // read as one control rather than two unrelated pictures. The icon shows the density the
        // tap will switch *to*, which is what the content description says.
        Icon(
            imageVector = if (density.isCondensed) {
                Icons.Default.DensityLarge
            } else {
                Icons.Default.DensitySmall
            },
            contentDescription = stringResource(labelResId)
        )
    }
}
