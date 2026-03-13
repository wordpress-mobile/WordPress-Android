package org.wordpress.android.ui.newstats.tagsandcategories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val VERTICAL_LINE_ALPHA = 0.3f

@Composable
fun TagTypeIcon(
    displayType: TagGroupDisplayType
) {
    Icon(
        imageVector = when (displayType) {
            TagGroupDisplayType.CATEGORY ->
                Icons.Outlined.Folder
            TagGroupDisplayType.TAG,
            TagGroupDisplayType.MIXED ->
                Icons.Outlined.Sell
        },
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme
            .onSurfaceVariant
    )
}

@Composable
fun ExpandedTagsSection(
    tags: List<TagUiItem>,
    startPadding: Dp = 24.dp
) {
    val lineColor = MaterialTheme.colorScheme.primary
        .copy(alpha = VERTICAL_LINE_ALPHA)

    Column(
        modifier = Modifier.padding(
            start = startPadding,
            top = 4.dp,
            bottom = 4.dp
        )
    ) {
        tags.forEachIndexed { index, tag ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(lineColor)
                )
                Spacer(
                    modifier = Modifier.width(12.dp)
                )
                TagTypeIcon(
                    displayType =
                        TagGroupDisplayType
                            .fromTagType(tag.tagType)
                )
                Spacer(
                    modifier = Modifier.width(8.dp)
                )
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography
                        .bodyMedium,
                    color = MaterialTheme.colorScheme
                        .onSurface
                )
            }
            if (index < tags.lastIndex) {
                Spacer(
                    modifier = Modifier.height(2.dp)
                )
            }
        }
    }
}
