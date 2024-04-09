package org.wordpress.android.ui.reader.views.compose.horizontalpostlist

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun HorizontalPostListItem(
    siteName: String,
    postDateLine: String,
    postTitle: String,
    onPostSiteImageClick: () -> Unit,
    onPostMoreMenuClick: () -> Unit,
) {
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
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColor.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // "•" separator
            Text(
                modifier = Modifier.padding(
                    horizontal = Margin.Small.value
                ),
                text = "•",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryElementColor,
            )
            // Time since it was posted
            Text(
                text = postDateLine,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryElementColor,
            )
            // More menu ("...")
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .padding(
                        horizontal = Margin.Small.value
                    ),
                onClick = {
                    onPostMoreMenuClick()
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_ellipsis_horizontal_squares),
                    contentDescription = stringResource(
                        R.string.show_more_desc
                    ),
                    tint = secondaryElementColor,
                )
            }
        }
        // Post title
        Text(
            modifier = Modifier.padding(top = Margin.Medium.value),
            text = postTitle,
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColor.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 25.sp,
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
                siteName = "This is a really long site name used for testing",
                postDateLine = "1h",
                postTitle = "This is a really really really long post title used for testing",
                onPostMoreMenuClick = {},
                onPostSiteImageClick = {},
            )
        }
    }
}
