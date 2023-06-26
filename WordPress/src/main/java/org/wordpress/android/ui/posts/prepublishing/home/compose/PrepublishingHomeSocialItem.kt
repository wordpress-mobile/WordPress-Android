package org.wordpress.android.ui.posts.prepublishing.home.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
    ) {
        Divider()

        // TODO thomashortadev check compose layout tutorials for a "flex" wrap implementation instead of Row
        //  if we want to wrap when the icons don't fit in the row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                Spacer(modifier = Modifier.width(4.dp))

                TrainOfIcons(iconModels = avatarModels)
            }
        }
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PrepublishingHomeSocialItemPreview() {
    AppTheme {
        PrepublishingHomeSocialItem(
            title = "Sharing to 3 accounts",
            description = "27/30 social shares remaining",
            avatarModels = listOf(
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_three, 0.36f),
                TrainOfIconsModel(R.drawable.login_prologue_second_asset_two, 0.36f),
                TrainOfIconsModel(R.drawable.login_prologue_third_asset_one),
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
