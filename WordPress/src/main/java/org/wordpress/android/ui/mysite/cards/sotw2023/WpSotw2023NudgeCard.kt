package org.wordpress.android.ui.mysite.cards.sotw2023

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbar
import org.wordpress.android.ui.mysite.cards.compose.MySiteCardToolbarContextMenuItem
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes

@Composable
fun WpSotw2023NudgeCard(
    model: WpSotw2023NudgeCardModel,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(
            modifier = Modifier.padding(bottom = Margin.Medium.value)
        ) {
            CardToolbar(model)

            Spacer(Modifier.height(Margin.Medium.value))

            Text(
                text = uiStringText(model.text),
                style = DashboardCardTypography.detailText,
                modifier = Modifier.padding(horizontal = Margin.ExtraLarge.value),
            )

            Spacer(Modifier.height(Margin.Small.value))

            TextButton(
                onClick = { model.onCtaClick.click() },
                modifier = Modifier.padding(horizontal = Margin.Small.value)
            ) {
                Text(
                    text = uiStringText(model.ctaText),
                    style = DashboardCardTypography.footerCTA,
                )
            }
        }
    }
}

@Composable
private fun CardToolbar(
    model: WpSotw2023NudgeCardModel
) {
    MySiteCardToolbar(
        contextMenuItems = listOf(
            MySiteCardToolbarContextMenuItem.Option(
                text = stringResource(R.string.my_site_dashboard_card_more_menu_hide_card),
                onClick = { model.onHideMenuItemClick.click() },
            )
        ),
    ) {
        Text(
            text = uiStringText(uiString = model.title),
            style = DashboardCardTypography.smallTitle,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WpSotw2023NudgeCardPreview() {
    AppThemeM2 {
        WpSotw2023NudgeCard(
            model = WpSotw2023NudgeCardModel(
                title = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_title),
                text = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_text),
                ctaText = UiStringRes(R.string.wp_sotw_2023_dashboard_nudge_cta),
                onHideMenuItemClick = ListItemInteraction.create {},
                onCtaClick = ListItemInteraction.create {},
            )
        )
    }
}
