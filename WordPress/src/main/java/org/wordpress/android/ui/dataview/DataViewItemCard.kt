package org.wordpress.android.ui.dataview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DummyDataViewItems.getDummyDataViewItems

/**
 * Provides a card for displaying a single [DataViewItem]
 */
@Composable
fun DataViewItemCard(
    item: DataViewItem,
    onItemClick: (DataViewItem) -> Unit,
) = Card(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onItemClick(item) },
    shape = RoundedCornerShape(4.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.image?.let { image ->
            RemoteImage(
                imageUrl = image.imageUrl,
                fallbackImageRes = image.fallbackImageRes,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        item.fields.forEach { field ->
            Column() {
                val style = when (field.fieldType) {
                    DataViewFieldType.TITLE -> MaterialTheme.typography.titleMedium
                    DataViewFieldType.DATE -> MaterialTheme.typography.bodySmall
                }
                val color = when (field.fieldType) {
                    DataViewFieldType.TITLE -> MaterialTheme.colorScheme.onSurface
                    DataViewFieldType.DATE -> MaterialTheme.colorScheme.outline
                }
                val fontWeight = when (field.fieldType) {
                    DataViewFieldType.TITLE -> FontWeight.Bold
                    DataViewFieldType.DATE -> FontWeight.Normal
                }
                Text(
                    text = field.value,
                    style = style,
                    color = color,
                    fontWeight = fontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                field.subValue?.let { subValue ->
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteImage(
    imageUrl: String?,
    fallbackImageRes: Int,
) {
    val modifier = Modifier
        .padding(end = 16.dp)
        .size(dimensionResource(R.dimen.jp_migration_user_avatar_size))
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surface)
    if (imageUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(id = fallbackImageRes),
            contentDescription = null,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .error(fallbackImageRes)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DataViewItemCardPreview() {
    AppThemeM3 {
        getDummyDataViewItems().forEach { item ->
            DataViewItemCard(item, onItemClick = {})
        }
    }
}
