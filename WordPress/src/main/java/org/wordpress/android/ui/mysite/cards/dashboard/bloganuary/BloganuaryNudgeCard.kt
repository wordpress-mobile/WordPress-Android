package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbar
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbarContextMenuItem

@Composable
fun BloganuaryNudgeCard(
    model: BloganuaryNudgeCardModel,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier,
    ) {
        Column {
            CardToolbar(model)
        }
    }
}

@Composable
private fun CardToolbar(
    model: BloganuaryNudgeCardModel,
) {
    MySiteCardToolbar(
        onContextMenuClick = { model.onMoreMenuClick.click() },
        contextMenuItems = listOf(
            MySiteCardToolbarContextMenuItem.Option(
                text = stringResource(id = R.string.my_site_dashboard_card_more_menu_hide_card),
                onClick = { model.onHideMenuItemClick.click() }
            )
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bloganuary_24dp),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colors.onSurface
        )
    }
}
