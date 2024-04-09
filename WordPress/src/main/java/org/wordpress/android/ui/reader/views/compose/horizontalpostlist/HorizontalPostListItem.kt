package org.wordpress.android.ui.reader.views.compose.horizontalpostlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun HorizontalPostListItem(
    siteName: String,
    postDateLine: String,
    postTitle: String,
    postExcerpt: String,
    onPostSiteImageClick: () -> Unit,
) {
    val black87AlphaColor = AppColor.Black.copy(
        alpha = 0.87F
    )
    Column(modifier = Modifier.width(240.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val secondaryElementColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.6F
            )
            // Site image
            HorizontalPostListItemSiteImage(
                modifier = Modifier.padding(
                    end = Margin.Small.value
                ),
                imageUrl = "",
                onClick = { onPostSiteImageClick() },
            )
            // Site name
            Text(
                modifier = Modifier.weight(1F),
                text = siteName,
                style = MaterialTheme.typography.labelLarge,
                color = black87AlphaColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // "•" separator
            Text(
                modifier = Modifier.padding(
                    horizontal = Margin.Small.value
                ),
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
            // Time since it was posted
            Text(
                text = postDateLine,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryElementColor,
            )
        }
        // Post title
        Text(
            modifier = Modifier.padding(top = Margin.Medium.value),
            text = postTitle,
            style = MaterialTheme.typography.titleMedium,
            color = AppColor.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // Post excerpt
        Text(
            modifier = Modifier.padding(top = Margin.Small.value),
            text = postExcerpt,
            style = MaterialTheme.typography.bodySmall,
            color = black87AlphaColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HorizontalPostListItemPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            HorizontalPostListItem(
                siteName = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien sed" +
                        " urna fermentum posuere. Vivamus in pretium nisl.",
                postDateLine = "1h",
                postTitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien " +
                        "sed urna fermentum posuere. Vivamus in pretium nisl.",
                postExcerpt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer pellentesque sapien" +
                        " sed urna fermentum posuere. Vivamus in pretium nisl.",
                onPostSiteImageClick = {},
            )
        }
    }
}
