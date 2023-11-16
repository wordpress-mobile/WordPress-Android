package org.wordpress.android.ui.posts.prepublishing.social.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.EditPostJetpackSocialConnectionsContainer
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.social.compose.DescriptionText
import org.wordpress.android.usecase.social.JetpackSocialFlow

@Composable
fun PrepublishingSocialScreen(
    state: JetpackSocialUiState.Loaded,
    modifier: Modifier = Modifier,
): Unit = with(state) {
    Column(modifier) {
        Divider()

        EditPostJetpackSocialConnectionsContainer(
            jetpackSocialConnectionDataList = jetpackSocialConnectionDataList,
            jetpackSocialFlow = JetpackSocialFlow.PRE_PUBLISHING,
            shareMessage = shareMessage,
            isShareMessageEnabled = isShareMessageEnabled,
            onShareMessageClick = onShareMessageClick,
        )

        if (showShareLimitUi) {
            DescriptionText(
                text = socialSharingModel.description,
                isLowOnShares = socialSharingModel.isLowOnShares,
                baseTextStyle = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(Margin.ExtraLarge.value),
            )

            PrimaryButton(
                text = state.subscribeButtonLabel,
                onClick = { state.onSubscribeClick(JetpackSocialFlow.PRE_PUBLISHING) },
                padding = PaddingValues(horizontal = Margin.ExtraLarge.value)
            )
        }
    }
}
