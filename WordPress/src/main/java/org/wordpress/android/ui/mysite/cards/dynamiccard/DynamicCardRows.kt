package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.ui.compose.styles.DashboardCardTypography
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.mysite.MySiteCardAndItem

@Composable
fun DynamicCardRows(rows: List<MySiteCardAndItem.Card.Dynamic.Row>, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        rows.forEach{ row -> Item(row) }
    }
}

@Composable
private fun Item(row: MySiteCardAndItem.Card.Dynamic.Row, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        row.iconUrl?.let { iconUrl ->
            Icon(iconUrl)
        }
        Column(modifier = Modifier.padding(start = row.iconUrl?.run { 12.dp } ?: 0.dp)) {
            row.title?.let { title ->
                Text(text = title, style = DashboardCardTypography.title)
            }
            row.description?.let { description ->
                Text(text = description, style = DashboardCardTypography.detailText)
            }
        }
    }
}

@Composable
private fun Icon(iconUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = iconUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        placeholder = ColorPainter(AppColor.Gray30),
        modifier = modifier.size(48.dp),
    )
}
