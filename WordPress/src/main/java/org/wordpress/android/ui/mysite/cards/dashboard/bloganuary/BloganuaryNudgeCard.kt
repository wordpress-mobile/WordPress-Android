package org.wordpress.android.ui.mysite.cards.dashboard.bloganuary

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.BloganuaryNudgeCardModel
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbar
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbarContextMenuItem
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString

@Composable
fun BloganuaryNudgeCard(
    model: BloganuaryNudgeCardModel,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column {
            CardToolbar(model)

            Spacer(Modifier.height(Margin.Medium.value))

            Text(
                text = uiStringText(model.title),
                style = DashboardCardTypography.subTitle,
                modifier = Modifier.padding(horizontal = Margin.ExtraLarge.value),
            )

            Spacer(Modifier.height(Margin.Small.value))

            Text(
                text = uiStringText(model.text),
                style = DashboardCardTypography.detailText,
                modifier = Modifier.padding(horizontal = Margin.ExtraLarge.value),
            )

            Spacer(Modifier.height(Margin.Small.value))

            TextButton(
                onClick = { model.onLearnMoreClick.click() },
                modifier = Modifier.padding(horizontal = Margin.Small.value)
            ) {
                Text(
                    text = stringResource(id = R.string.bloganuary_dashboard_nudge_learn_more),
                    style = DashboardCardTypography.footerCTA,
                )
            }

            Spacer(Modifier.height(Margin.Medium.value))
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

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BloganuaryNudgeCardPreview() {
    AppThemeM2 {
        BloganuaryNudgeCard(
            model = BloganuaryNudgeCardModel(
                UiString.UiStringRes(R.string.bloganuary_dashboard_nudge_title_december),
                UiString.UiStringRes(R.string.bloganuary_dashboard_nudge_text),
                onLearnMoreClick = ListItemInteraction.create { },
                onMoreMenuClick = ListItemInteraction.create { },
                onHideMenuItemClick = ListItemInteraction.create { },
            )
        )
    }
}
