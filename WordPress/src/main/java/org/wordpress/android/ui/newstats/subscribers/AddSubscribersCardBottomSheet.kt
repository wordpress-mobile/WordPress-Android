package org.wordpress.android.ui.newstats.subscribers

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscribersCardBottomSheet(
    sheetState: SheetState,
    availableCards: List<SubscribersCardType>,
    onDismiss: () -> Unit,
    onCardSelected: (SubscribersCardType) -> Unit
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
                text = stringResource(
                    R.string.stats_add_card_title
                ),
                style = MaterialTheme
                    .typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )

            if (availableCards.isEmpty()) {
                Text(
                    text = stringResource(
                        R.string.stats_all_cards_visible
                    ),
                    style = MaterialTheme
                        .typography.bodyMedium,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                )
            } else {
                availableCards.forEach { cardType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCardSelected(cardType)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme
                                .colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                        )
                        Spacer(
                            modifier = Modifier
                                .width(16.dp)
                        )
                        Text(
                            text = stringResource(
                                cardType
                                    .displayNameResId
                            ),
                            style = MaterialTheme
                                .typography.bodyLarge,
                            color = MaterialTheme
                                .colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
