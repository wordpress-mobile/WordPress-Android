package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.card.UnelevatedCard
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.domains.management.M3Theme
import org.wordpress.android.ui.domains.management.success
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
                CardHeader(
                    title = card.title,
                    onHideMenuClicked = { card.onHideMenuItemClick.click() }
                )
                card.image?.let { imageUrl -> CardFeatureImage(imageUrl) }
                if (card.rows.isNotEmpty()) CardRow(card.rows)
                card.action?.let { action ->
                    TextButton(
                        modifier = Modifier.padding(start = 4.dp),
                        onClick = { card.onCtaClick.click() },
                    ) {
                        Text(
                            text = action,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.success
                            ),
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun CardHeader(
    title: String?,
    onHideMenuClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            content = {
                title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    )
                }
            }
        )
        IconButton(
            modifier = Modifier.size(32.dp), // to match the icon in my_site_card_toolbar.xml
            onClick = onHideMenuClicked
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(id = R.string.more),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium),
            )
        }
    }
}

@Composable
private fun CardFeatureImage(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(AppColor.Gray30),
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(6.dp))
            .fillMaxWidth()
            .aspectRatio(2f)
    )
}

@Composable
private fun CardRow(rows: List<MySiteCardAndItem.Card.Dynamic.Row>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        items(items = rows) { row -> CardRow(row) }
    }
}

@Composable
private fun CardRow(row: MySiteCardAndItem.Card.Dynamic.Row) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        row.iconUrl?.let { iconUrl ->
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                placeholder = ColorPainter(AppColor.Gray30),
                modifier = Modifier.size(48.dp),
            )
        }
        Column(modifier = Modifier.padding(start = row.iconUrl?.run { 12.dp } ?: 0.dp)) {
            row.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            row.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                )
            }
        }
    }
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
