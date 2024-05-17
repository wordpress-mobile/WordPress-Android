package org.wordpress.android.ui.reader.views.compose

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.designsystem.footnote
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun ReaderAnnouncementCard(
    items: List<ReaderAnnouncementCardItemData>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Margin.ExtraLarge.value),
        verticalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value),
    ) {
        // Title
        Text(
            text = stringResource(R.string.reader_announcement_card_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // Items
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value)
        ) {
            items(
                items = items,
            ) {
                ReaderAnnouncementCardItem(it)
            }
        }
        // Done button
        Button(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { },
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.reader_btn_done),
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ReaderAnnouncementCardItem(data: ReaderAnnouncementCardItemData) {
    val baseColor = if (isSystemInDarkTheme()) AppColor.White else AppColor.Black
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 54.dp, minHeight = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconBackgroundColor = MaterialTheme.colorScheme.onSurface
        Icon(
            modifier = Modifier
                .padding(
                    start = Margin.Large.value,
                    end = Margin.Large.value
                )
                .drawBehind {
                    drawCircle(
                        color = iconBackgroundColor,
                        radius = this.size.maxDimension,
                    )
                },
            painter = painterResource(data.iconRes),
            tint = MaterialTheme.colorScheme.surface,
            contentDescription = null
        )
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                modifier = Modifier.padding(
                    start = Margin.Large.value,
                ),
                text = stringResource(data.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = baseColor,
            )
            val secondaryElementColor = baseColor.copy(
                alpha = 0.6F
            )
            Text(
                modifier = Modifier.padding(
                    start = Margin.Large.value,
                ),
                text = stringResource(data.descriptionRes),
                style = MaterialTheme.typography.footnote,
                color = secondaryElementColor,
            )
        }
    }
}

data class ReaderAnnouncementCardItemData(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)


@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReaderTagsFeedPostListItemPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ReaderAnnouncementCard(
                items = listOf(
                    ReaderAnnouncementCardItemData(
                        iconRes = R.drawable.ic_wifi_off_24px,
                        titleRes = R.string.reader_tags_display_name,
                        descriptionRes = R.string.reader_tags_feed_loading_error_description,
                    ),
                    ReaderAnnouncementCardItemData(
                        iconRes = R.drawable.ic_wifi_off_24px,
                        titleRes = R.string.reader_tags_display_name,
                        descriptionRes = R.string.reader_tags_feed_loading_error_description,
                    ),
                    ReaderAnnouncementCardItemData(
                        iconRes = R.drawable.ic_wifi_off_24px,
                        titleRes = R.string.reader_tags_display_name,
                        descriptionRes = R.string.reader_tags_feed_loading_error_description,
                    ),
                )
            )
        }
    }
}
