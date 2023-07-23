package org.wordpress.android.ui.posts.social.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PostSocialSharingItem(
    title: String,
    description: String,
    iconModels: List<TrainOfIconsModel>,
    modifier: Modifier = Modifier,
    isLowOnShares: Boolean = false,
    backgroundColor: Color = MaterialTheme.colors.surface
) {
    SocialContainer(
        iconCount = iconModels.size,
        modifier = Modifier
            .background(backgroundColor)
            .then(modifier),
    ) { textColumnModifier ->
        Column(modifier = textColumnModifier) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1.copy(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(Margin.ExtraSmall.value))

            DescriptionText(
                text = description,
                isLowOnShares = isLowOnShares,
            )
        }

        if (iconModels.isNotEmpty()) {
            Spacer(modifier = Modifier.size(Margin.Medium.value))

            TrainOfIcons(iconModels = iconModels, iconBorderColor = backgroundColor)
        }
    }
}

@Composable
private fun SocialContainer(
    iconCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (textColumnModifier: Modifier) -> Unit,
) {
    if (iconCount > 2) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = modifier,
        ) {
            content(Modifier)
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            content(Modifier.weight(1f))
        }
    }
}

private val warningColor: Color
    @ReadOnlyComposable
    @Composable
    get() = AppColor.Yellow50

private val lowOnSharesDescriptionStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.body1
        .copy(color = warningColor)

private val defaultDescriptionStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.body1
        .copy(color = AppColor.Gray30)

@Composable
private fun DescriptionText(
    text: String,
    isLowOnShares: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Margin.Small.value)
    ) {
        if (isLowOnShares) {
            Icon(
                painterResource(R.drawable.ic_notice_white_24dp),
                contentDescription = null,
                tint = warningColor,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = text,
            style = if (isLowOnShares) lowOnSharesDescriptionStyle else defaultDescriptionStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Light Mode", locale = "en")
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "RTL", locale = "ar")
@Composable
private fun PostSocialSharingItemPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
        ) {
            PostSocialSharingItem(
                title = "Sharing to 2 of 3 accounts",
                description = "27/30 social shares remaining",
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_facebook),
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            PostSocialSharingItem(
                title = "Sharing to 2 of 3 accounts",
                description = "27/30 social shares remaining with a very long text that should be truncated",
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_facebook),
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Divider()

            PostSocialSharingItem(
                title = "Sharing to 3 of 5 accounts",
                description = "27/30 social shares remaining",
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_facebook, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_mastodon, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_twitter),
                    TrainOfIconsModel(R.drawable.ic_social_linkedin),
                    TrainOfIconsModel(R.drawable.ic_social_instagram),
                    TrainOfIconsModel(R.drawable.ic_social_tumblr),
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Divider()

            PostSocialSharingItem(
                title = "Not sharing to social",
                description = "0/30 social shares remaining",
                isLowOnShares = true,
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}
