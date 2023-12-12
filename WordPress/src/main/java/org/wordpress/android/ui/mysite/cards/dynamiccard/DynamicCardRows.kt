package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.mysite.MySiteCardAndItem

@Composable
fun DynamicCardRows(rows: List<MySiteCardAndItem.Card.Dynamic.Row>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        items(items = rows) { row -> Item(row) }
    }
}

@Composable
private fun Item(row: MySiteCardAndItem.Card.Dynamic.Row) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        row.iconUrl?.let { iconUrl ->
            Icon(iconUrl)
        }
        Column(modifier = Modifier.padding(start = row.iconUrl?.run { 12.dp } ?: 0.dp)) {
            row.title?.let { title ->
                Title(title)
            }
            row.description?.let { description ->
                Description(description)
            }
        }
    }
}

@Composable
private fun Icon(iconUrl: String) {
    AsyncImage(
        model = iconUrl,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        placeholder = ColorPainter(AppColor.Gray30),
        modifier = Modifier.size(48.dp),
    )
}

@Composable
private fun Title(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun Description(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium)
        )
    )
}
