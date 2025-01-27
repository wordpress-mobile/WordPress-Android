package org.wordpress.android.ui.posts.prepublishing.home.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.components.buttons.SecondaryButtonM3
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.publicize.PublicizeServiceIcon
import org.wordpress.android.usecase.social.JetpackSocialFlow

@Composable
fun PrepublishingHomeSocialNoConnectionsItem(
    connectionIconModels: List<TrainOfIconsModel>,
    onConnectClick: (JetpackSocialFlow) -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    Column(
        modifier = Modifier
            .background(backgroundColor)
            .then(modifier),
    ) {
        TrainOfIcons(
            iconModels = connectionIconModels,
            iconBorderColor = backgroundColor,
        )

        Spacer(Modifier.height(Margin.ExtraLarge.value))

        Text(
            text = stringResource(R.string.prepublishing_nudges_social_new_connection_text),
            style = MaterialTheme.typography.titleMedium.copy(color = AppColor.Gray30),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Margin.Medium.value))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButtonM3(
                text = stringResource(R.string.prepublishing_nudges_social_new_connection_cta),
                onClick = { onConnectClick(JetpackSocialFlow.PRE_PUBLISHING) },
                padding = PaddingValues(0.dp),
                contentPadding = PaddingValues(0.dp),
                fillMaxWidth = false,
                modifier = Modifier.weight(1f, fill = false)
            )

            // min spacing between buttons
            Spacer(Modifier.width(Margin.Medium.value))

            SecondaryButtonM3(
                text = stringResource(R.string.button_not_now),
                onClick = onDismissClick,
                padding = PaddingValues(0.dp),
                contentPadding = PaddingValues(0.dp),
                fillMaxWidth = false,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PrepublishingHomeSocialNoConnectionsItemPreview() {
    AppThemeM3 {
        PrepublishingHomeSocialNoConnectionsItem(
            connectionIconModels = PublicizeServiceIcon.values().map { TrainOfIconsModel(it.iconResId) },
            onConnectClick = { /*TODO*/ },
            onDismissClick = { /*TODO*/ },
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 24.dp,
            ),
        )
    }
}
