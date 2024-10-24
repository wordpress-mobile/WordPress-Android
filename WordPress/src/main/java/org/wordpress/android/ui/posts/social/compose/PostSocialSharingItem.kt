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
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PostSocialSharingItem(
    model: PostSocialSharingModel,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface
) {
    SocialContainer(
        iconCount = model.iconModels.size,
        modifier = Modifier
            .background(backgroundColor)
            .then(modifier),
    ) { textColumnModifier ->
        Column(modifier = textColumnModifier) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.subtitle1.copy(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (model.description.isNotBlank()) {
                Spacer(Modifier.height(Margin.ExtraSmall.value))

                DescriptionText(
                    text = model.description,
                    isLowOnShares = model.isLowOnShares,
                    baseTextStyle = MaterialTheme.typography.body2
                        .copy(color = AppColor.Gray30),
                )
            }
        }

        if (model.iconModels.isNotEmpty()) {
            Spacer(modifier = Modifier.size(Margin.Medium.value))

            TrainOfIcons(iconModels = model.iconModels, iconBorderColor = backgroundColor)
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

@Composable
fun DescriptionText(
    text: String,
    isLowOnShares: Boolean,
    modifier: Modifier = Modifier,
    baseTextStyle: TextStyle = MaterialTheme.typography.body2,
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
            style = baseTextStyle.copy(
                color = if (isLowOnShares) warningColor else baseTextStyle.color,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

data class PostSocialSharingModel(
    val title: String,
    val description: String,
    val iconModels: List<TrainOfIconsModel>,
    val isLowOnShares: Boolean = false,
)

@Preview(name = "Light Mode", locale = "en")
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "RTL", locale = "ar")
@Composable
private fun PostSocialSharingItemPreview() {
    AppThemeM2 {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
        ) {
            val model = PostSocialSharingModel(
                title = "Sharing to 2 of 3 accounts",
                description = "27/30 social shares remaining",
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_facebook),
                ),
            )
            PostSocialSharingItem(
                model = model,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            val model2 = PostSocialSharingModel(
                title = "Sharing to 2 of 3 accounts",
                description = "27/30 social shares remaining with a very long text that should be truncated",
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                    TrainOfIconsModel(R.drawable.ic_social_facebook),
                ),
            )
            PostSocialSharingItem(
                model = model2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )

            Divider()

            val model3 = PostSocialSharingModel(
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
            )
            PostSocialSharingItem(
                model = model3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Divider()

            val model4 = PostSocialSharingModel(
                title = "Not sharing to social",
                description = "0/30 social shares remaining",
                iconModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, ContentAlpha.disabled),
                ),
                isLowOnShares = true,
            )
            PostSocialSharingItem(
                model = model4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
