package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.utils.ListItemInteraction

@Composable
fun DynamicDashboardCard(
    card: MySiteCardAndItem.Card.Dynamic,
    modifier: Modifier = Modifier,
) {
    UnelevatedCard(
        modifier = modifier,
        content = {
            Column(
                Modifier.padding(top = 8.dp, bottom = 8.dp)
            ) {
                DynamicCardHeader(
                    title = card.title,
                    onHideMenuClicked = { card.onHideMenuItemClick.click() }
                )
                card.image?.let { imageUrl ->
                    DynamicCardFeatureImage(imageUrl)
                }
                if (card.rows.isNotEmpty()) {
                    DynamicCardRows(card.rows)
                }
                card.action?.let { action ->
                    DynamicCardCallToActionButton(text = action, onClicked = { card.onCtaClick.click() })
                }
            }
        }
    )
}

@Preview
@Composable
fun DynamicDashboardCardPreview() {
    M3Theme {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                order = MySiteCardAndItem.Card.Dynamic.Order.TOP,
                title = "Card Title",
                image = "https://picsum.photos/200/300",
                action = "Call to Action",
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
                onCtaClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithFeatureAndDescriptionPreview() {
    M3Theme {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                order = MySiteCardAndItem.Card.Dynamic.Order.TOP,
                title = null,
                image = "https://picsum.photos/200/300",
                action = "See yours now",
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = null,
                        title = null,
                        description = "Review your blog's impactful year with key \n" +
                                "stats on viewership, engagement, and community growth."
                    )
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
                onCtaClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithFeatureAndSubtitleAndDescriptionPreview() {
    M3Theme {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                order = MySiteCardAndItem.Card.Dynamic.Order.TOP,
                title = null,
                image = "https://picsum.photos/200/300",
                action = "Find out more",
                rows = listOf(
                    MySiteCardAndItem.Card.Dynamic.Row(
                        iconUrl = null,
                        title = "New and Improved\nJetpack Mobile Editor",
                        description = "Updated colours and icons, streamlined typing" +
                                ", unified block controls, drag and drop."
                    )
                ),
                onHideMenuItemClick = ListItemInteraction.create {},
                onCtaClick = ListItemInteraction.create {},
            )
        )
    }
}

@Preview
@Composable
fun DynamicDashboardCardWithTitleAndCompleteRowsPreview() {
    M3Theme {
        DynamicDashboardCard(
            card = MySiteCardAndItem.Card.Dynamic(
                id = "id",
                order = MySiteCardAndItem.Card.Dynamic.Order.TOP,
                title = "What's New in Jetpack",
                image = null,
                action = "Find out more",
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
                onCtaClick = ListItemInteraction.create {},
            )
        )
    }
}
