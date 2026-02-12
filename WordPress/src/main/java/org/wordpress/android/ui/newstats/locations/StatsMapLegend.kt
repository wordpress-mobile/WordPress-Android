package org.wordpress.android.ui.newstats.countries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.util.formatStatValue
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * A shared map legend component that displays a gradient bar with min/max values.
 * Uses the same colors as the GeoChart map (stats_map_activity_low/high).
 *
 * @param minViews The minimum views value to display
 * @param maxViews The maximum views value to display
 * @param modifier Optional modifier for the legend
 */
@Composable
fun StatsMapLegend(
    minViews: Long,
    maxViews: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Use the same colors as the map (stats color resources)
    val colorLow = ComposeColor(ContextCompat.getColor(context, R.color.stats_map_activity_low))
    val colorHigh = ComposeColor(ContextCompat.getColor(context, R.color.stats_map_activity_high))

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatStatValue(minViews),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(colorLow, colorHigh)
                    )
                )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatStatValue(maxViews),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
