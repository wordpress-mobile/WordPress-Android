package org.wordpress.android.ui.posts.prepublishing.home.compose

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun PrepublishingHomeSocialItem(
    title: String,
    description: String,
    avatarModels: List<TrainOfIconsModel>,
    modifier: Modifier = Modifier,
    isLowOnShares: Boolean = false
) {
    SocialContainer(
        avatarCount = avatarModels.size,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(Margin.ExtraLarge.value),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(Margin.ExtraSmall.value))

            DescriptionText(
                text = description,
                isLowOnShares = isLowOnShares,
            )
        }

        if (avatarModels.isNotEmpty()) {
            Spacer(modifier = Modifier.size(Margin.Medium.value))

            TrainOfIcons(iconModels = avatarModels)
        }
    }
}

@Composable
private fun SocialContainer(
    avatarCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (avatarCount > 2) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = modifier,
        ) {
            content()
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            content()
        }
    }
}

private val lowOnSharesDescriptionStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.body2.copy(
        color = AppColor.Yellow50,
    )

private val defaultDescriptionStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.body2.copy(
        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
    )

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
                tint = AppColor.Yellow50,
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

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "RTL", locale = "ar")
@Composable
fun PrepublishingHomeSocialItemPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.background)
        ) {
            PrepublishingHomeSocialItem(
                title = "Sharing to 2 of 3 accounts",
                description = "27/30 social shares remaining",
                avatarModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_facebook),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            PrepublishingHomeSocialItem(
                title = "Sharing to 3 of 5 accounts",
                description = "27/30 social shares remaining",
                avatarModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_facebook, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_mastodon, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_twitter),
                    TrainOfIconsModel(R.drawable.ic_social_linkedin),
                    TrainOfIconsModel(R.drawable.ic_social_instagram),
                    TrainOfIconsModel(R.drawable.ic_social_tumblr),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            PrepublishingHomeSocialItem(
                title = "Not sharing to social",
                description = "0/30 social shares remaining",
                isLowOnShares = true,
                avatarModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
