package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbar
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbarContextMenuItem

@Composable
fun DynamicCardHeader(
    title: String?,
    onHideMenuClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MySiteCardToolbar(
        modifier = modifier.padding(bottom = 8.dp),
        contextMenuItems = listOf(
            MySiteCardToolbarContextMenuItem.Option(
                text = stringResource(R.string.my_site_dashboard_card_more_menu_hide_card),
                onClick = onHideMenuClicked,
            )
        ),
    ) {
        Title(title = title, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Title(title: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(end = 16.dp),
        content = {
            title?.let { title ->
                Text(text = title, style = DashboardCardTypography.subTitle)
            }
        }
    )
}
