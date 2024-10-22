package org.wordpress.android.ui.mysite.cards.dynamiccard

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.Dynamic.ActionSource
import org.wordpress.android.ui.utils.ListItemInteraction

@Composable
fun DynamicDashboardCard(
    card: MySiteCardAndItem.Card.Dynamic,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier
            .run {
                if (card.action != null) clickable { card.action.onCardClick.click() } else this
            },
        content = {
            Column(
                Modifier.padding(bottom = 8.dp)
            ) {
                val isCtaInvisible = card.action !is ActionSource.CardOrButton
                DynamicCardHeader(
                    title = card.title,
                    onHideMenuClicked = { card.onHideMenuItemClick.click() }
                )
                card.image?.let { imageUrl ->
                    DynamicCardFeatureImage(
                        imageUrl,
                        modifier = Modifier.run {
                            if (isCtaInvisible && card.rows.isEmpty()) padding(bottom = 8.dp) else this
                        }
                    )
                }
                if (card.rows.isNotEmpty()) {
                    DynamicCardRows(
                        rows = card.rows,
                        modifier = Modifier.run { if (isCtaInvisible) padding(bottom = 8.dp) else this }
                    )
                }
                (card.action as? ActionSource.CardOrButton)?.title?.let { title ->
                    DynamicCardCallToActionButton(text = title, onClicked = { card.action.onButtonClick.click() })
                }
            }
        }
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DynamicDashboardCardPreview() {
    AppThemeM2 {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                title = "Card Title",
                image = "https://picsum.photos/200/300",
                action = ActionSource.CardOrButton(
                    title = "Call to Action", url = "",
                    onCardClick = ListItemInteraction.create {},
                    onButtonClick = ListItemInteraction.create {},
                ),
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = "",
                        title = "Title first",
                        description = "Description first"
                    ),
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = "",
                        title = "Title second",
                        description = "Description second"
                    ),
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithFeatureAndDescriptionPreview() {
    AppThemeM2 {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                title = null,
                image = "https://picsum.photos/200/300",
                action = ActionSource.CardOrButton(
                    title = "See yours now",
                    url = "",
                    onCardClick = ListItemInteraction.create {},
                    onButtonClick = ListItemInteraction.create {},
                ),
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = null,
                        title = null,
                        description = "Review your blog's impactful year with key \n" +
                                "stats on viewership, engagement, and community growth."
                    )
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithFeatureAndSubtitleAndDescriptionPreview() {
    AppThemeM2 {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                title = null,
                image = "https://picsum.photos/200/300",
                action = ActionSource.CardOrButton(
                    title = "Find out more",
                    url = "",
                    onCardClick = ListItemInteraction.create {},
                    onButtonClick = ListItemInteraction.create {},
                ),
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = null,
                        title = "New and Improved\nJetpack Mobile Editor",
                        description = "Updated colours and icons, streamlined typing" +
                                ", unified block controls, drag and drop."
                    )
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithNoCta() {
    AppThemeM2 {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                title = null,
                image = "https://picsum.photos/200/300",
                action = ActionSource.Card(
                    url = "",
                    onCardClick = ListItemInteraction.create {},
                ),
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = null,
                        title = "New and Improved\nJetpack Mobile Editor",
                        description = "Updated colours and icons, streamlined typing" +
                                ", unified block controls, drag and drop."
                    )
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardWithFeatureImageOnly() {
    AppThemeM2 {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                title = null,
                image = "https://picsum.photos/200/300",
                action = ActionSource.Card(
                    url = "",
                    onCardClick = ListItemInteraction.create {},
                ),
                rows = listOf(),
                onHideMenuItemClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithTitleAndCompleteRowsPreview() {
    AppThemeM2 {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                title = "What's New in Jetpack",
                image = null,
                action = ActionSource.CardOrButton(
                    title = "Find out more", url = "",
                    onCardClick = ListItemInteraction.create {},
                    onButtonClick = ListItemInteraction.create {},
                ),
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = "url",
                        title = "Domain Management",
                        description = "We added a space for all your domains in the ‘Me’ tab."
                    ),
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = "url",
                        title = "Media Enhancements",
                        description = "Rebuilt for improved performance and new interactions."
                    ),
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = "url",
                        title = "Posts and Pages",
                        description = "Streamlined design and improved options menus."
                    ),
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
            )
        )
    }
}
