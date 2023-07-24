package org.wordpress.android.ui.posts

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.publicize.PublicizeServiceIcon

@Composable
fun EditPostSettingsJetpackSocialNoConnections(
    trainOfIconsModels: List<TrainOfIconsModel>,
    message: String,
    connectProfilesButtonLabel: String,
    onConnectProfilesCLick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
) {
    Column(
        modifier = Modifier
            .background(backgroundColor)
            .padding(
                PaddingValues(
                    vertical = Margin.ExtraLarge.value
                )
            )
            .then(modifier),
    ) {
        TrainOfIcons(
            iconModels = trainOfIconsModels,
            iconBorderColor = backgroundColor,
            modifier = Modifier.padding(
                PaddingValues(
                    horizontal = Margin.ExtraLarge.value
                )
            ),
        )
        Spacer(Modifier.height(Margin.ExtraLarge.value))
        Text(
            text = message,
            style = MaterialTheme.typography.body1.copy(color = AppColor.Gray30),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    PaddingValues(
                        horizontal = Margin.ExtraLarge.value
                    )
                ),
        )
        Spacer(Modifier.height(Margin.Medium.value))
        PrimaryButton(
            text = connectProfilesButtonLabel,
            onClick = onConnectProfilesCLick,
            fillMaxWidth = false,
            padding = PaddingValues(
                horizontal = Margin.ExtraLarge.value
            ),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EditPostSettingsJetpackSocialNoConnectionsPreview() {
    AppThemeEditor {
        EditPostSettingsJetpackSocialNoConnections(
            trainOfIconsModels = PublicizeServiceIcon.values().map { TrainOfIconsModel(it.iconResId) },
            message = "Increase your traffic by auto-sharing your posts with your friends on social media.",
            connectProfilesButtonLabel = "Connect your social profiles",
            onConnectProfilesCLick = {}
        )
    }
}
