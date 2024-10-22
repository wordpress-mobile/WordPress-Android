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
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun ReaderAnnouncementCard(
    items: List<ReaderAnnouncementCardItemData>,
    onAnnouncementCardDoneClick: () -> Unit,
) {
    val primaryColor = if (isSystemInDarkTheme()) AppColor.White else AppColor.Black
    val secondaryColor = if (isSystemInDarkTheme()) AppColor.Black else AppColor.White
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
            color = primaryColor,
        )
        // Items
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Margin.ExtraLarge.value)
        ) {
            items.forEach {
                ReaderAnnouncementCardItem(it)
            }
        }
        // Done button
        Button(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { onAnnouncementCardDoneClick() },
            elevation = ButtonDefaults.elevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = primaryColor,
            ),
        ) {
            Text(
                text = stringResource(id = R.string.reader_btn_done),
                color = secondaryColor,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ReaderAnnouncementCardItem(data: ReaderAnnouncementCardItemData) {
    val primaryColor = if (isSystemInDarkTheme()) AppColor.White else AppColor.Black
    val secondaryColor = if (isSystemInDarkTheme()) AppColor.Black else AppColor.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 54.dp, minHeight = 54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconBackgroundColor = primaryColor
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
            tint = secondaryColor,
            contentDescription = null
        )
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                modifier = Modifier.padding(
                    start = Margin.Large.value,
                ),
                text = stringResource(data.titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = primaryColor,
            )
            val secondaryElementColor = primaryColor.copy(
                alpha = 0.6F
            )
            Text(
                modifier = Modifier.padding(
                    start = Margin.Large.value,
                ),
                text = stringResource(data.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
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
    AppThemeM2 {
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
                ),
                onAnnouncementCardDoneClick = {},
            )
        }
    }
}
