package org.wordpress.android.ui.mysite.cards.sotw2023

import android.content.res.Configuration
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.WpSotw2023NudgeCardModel
import org.wordpress.android.ui.utils.ListItemInteraction

@Suppress("UNUSED_PARAMETER")
@Composable
fun WpSotw2023NudgeCard(
    model: WpSotw2023NudgeCardModel,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier,
    ) {
        Text("SOTW 2023")
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WpSotw2023NudgeCardPreview() {
    AppTheme {
        WpSotw2023NudgeCard(
            model = WpSotw2023NudgeCardModel(
                onMoreMenuClick = ListItemInteraction.create {},
                onHideMenuItemClick = ListItemInteraction.create {},
                onCtaClick = ListItemInteraction.create {},
            )
        )
    }
}
