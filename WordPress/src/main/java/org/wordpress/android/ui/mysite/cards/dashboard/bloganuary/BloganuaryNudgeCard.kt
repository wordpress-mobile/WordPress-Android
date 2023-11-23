package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.utils.ListItemInteraction

@Composable
fun BloganuaryNudgeCard(
    model: BloganuaryNudgeCardModel,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bloganuary_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.onSurface
                )
                CardDropDownMenu(
                    onMoreMenuClick = model.onMoreMenuClick,
                    onHideItemClick = model.onHideMenuItemClick,
                )
            }
        }
    }
}

@Composable
private fun CardDropDownMenu(
    onMoreMenuClick: ListItemInteraction,
    onHideItemClick: ListItemInteraction,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        var isExpanded by remember { mutableStateOf(false) }

        IconButton(
            onClick = {
                isExpanded = true
                onMoreMenuClick.click()
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.more),
                tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier
                .background(MaterialTheme.colors.surface.copy(alpha = ContentAlpha.high))
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.my_site_dashboard_card_more_menu_hide_card)) },
                onClick = {
                    isExpanded = false
                    onHideItemClick.click()
                }
            )
        }
    }
}
