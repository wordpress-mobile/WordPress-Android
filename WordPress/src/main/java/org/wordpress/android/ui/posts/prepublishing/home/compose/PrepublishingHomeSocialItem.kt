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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun PrepublishingHomeSocialItem(
    title: String,
    description: String,
    avatarModels: List<TrainOfIconsModel>,
    modifier: Modifier = Modifier
) {
    SocialContainer(
        avatarCount = avatarModels.size,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (avatarModels.isNotEmpty()) {
            Spacer(modifier = Modifier.size(8.dp))

            TrainOfIcons(iconModels = avatarModels)
        }
    }
}

@Composable
fun SocialContainer(
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

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PrepublishingHomeSocialItemPreviewHorizontal() {
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
                    TrainOfIconsModel(R.drawable.ic_social_tumblr, 0.36f),
                    TrainOfIconsModel(R.drawable.ic_social_facebook),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            PrepublishingHomeSocialItem(
                title = "Sharing to 1 of 3 accounts",
                description = "27/30 social shares remaining",
                avatarModels = listOf(
                    TrainOfIconsModel(R.drawable.ic_social_facebook, 0.36f),
                    TrainOfIconsModel(R.drawable.ic_social_mastodon, 0.36f),
                    TrainOfIconsModel(R.drawable.ic_social_tumblr),
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
