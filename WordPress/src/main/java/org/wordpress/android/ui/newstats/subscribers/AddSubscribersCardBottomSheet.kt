package org.wordpress.android.ui.newstats.subscribers

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import org.wordpress.android.ui.newstats.components.AddCardBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscribersCardBottomSheet(
    sheetState: SheetState,
    availableCards: List<SubscribersCardType>,
    onDismiss: () -> Unit,
    onCardSelected: (SubscribersCardType) -> Unit
) {
    AddCardBottomSheet(
        sheetState = sheetState,
        availableCards = availableCards,
        getDisplayNameResId = { it.displayNameResId },
        onDismiss = onDismiss,
        onCardSelected = onCardSelected
    )
}
