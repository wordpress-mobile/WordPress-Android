package org.wordpress.android.ui.reader.views.compose.tagsfeed

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

private val ThinLineHeight = 10.dp
private val ThickLineHeight = 16.dp

@Composable
fun ReaderTagsFeedPostListItemLoading() {
    val contentColor = if (isSystemInDarkTheme()) {
        AppColor.White.copy(alpha = 0.12F)
    } else {
        AppColor.Black.copy(alpha = 0.08F)
    }
    Column(
        modifier = Modifier
            .width(ReaderTagsFeedComposeUtils.PostItemWidth)
            .height(ReaderTagsFeedComposeUtils.PostItemHeight),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Site info placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(ThinLineHeight)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(contentColor),
            )
        }

        // Content row placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Margin.Medium.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Post title and excerpt Column placeholder
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(Margin.Medium.value),
            ) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(ThickLineHeight)
                        .clip(shape = RoundedCornerShape(16.dp))
                        .background(contentColor),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(ThickLineHeight)
                        .clip(shape = RoundedCornerShape(16.dp))
                        .background(contentColor),
                )
            }

            // Image placeholder
            Box(
                modifier = Modifier
                    .size(ReaderTagsFeedComposeUtils.POST_ITEM_IMAGE_SIZE)
                    .clip(shape = RoundedCornerShape(8.dp))
                    .background(contentColor),
            )
        }

        // Likes and comments + actions placeholder
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Margin.MediumLarge.value),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(ThinLineHeight)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(contentColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(ThinLineHeight)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(contentColor),
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedPostListItemLoadingPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(Margin.ExtraMediumLarge.value),
            ) {
                items(5) {
                    ReaderTagsFeedPostListItemLoading()
                }
            }
        }
    }
}
