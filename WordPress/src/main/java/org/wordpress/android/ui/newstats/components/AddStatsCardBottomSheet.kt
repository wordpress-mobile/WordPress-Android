package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource

/**
 * Bottom sheet for adding stats cards.
 * Shows a list of available (hidden) cards that can be added, including Most Viewed data sources.
 *
 * @param sheetState The state of the bottom sheet
 * @param availableCards List of card types that can be added (currently hidden, excluding MOST_VIEWED)
 * @param availableMostViewedDataSources List of Most Viewed data sources that can be added
 * @param onDismiss Callback invoked when the sheet is dismissed
 * @param onCardSelected Callback invoked when a card is selected to be added
 * @param onMostViewedDataSourceSelected Callback invoked when a Most Viewed data source is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStatsCardBottomSheet(
    sheetState: SheetState,
    availableCards: List<StatsCardType>,
    availableMostViewedDataSources: List<MostViewedDataSource>,
    onDismiss: () -> Unit,
    onCardSelected: (StatsCardType) -> Unit,
    onMostViewedDataSourceSelected: (MostViewedDataSource) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_add_card_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val hasAvailableItems = availableCards.isNotEmpty() ||
                availableMostViewedDataSources.isNotEmpty()

            if (!hasAvailableItems) {
                Text(
                    text = stringResource(R.string.stats_all_cards_visible),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                // Regular cards (excluding MOST_VIEWED which is handled separately)
                availableCards.forEach { cardType ->
                    AddCardItem(
                        label = stringResource(cardType.displayNameResId),
                        onClick = {
                            onCardSelected(cardType)
                            onDismiss()
                        }
                    )
                }

                // Most Viewed data sources
                availableMostViewedDataSources.forEach { dataSource ->
                    AddCardItem(
                        label = stringResource(dataSource.labelResId),
                        onClick = {
                            onMostViewedDataSourceSelected(dataSource)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCardItem(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
