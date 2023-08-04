package org.wordpress.android.ui.posts.prepublishing.social.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState

@Composable
fun PrepublishingSocialScreen(
    state: JetpackSocialUiState.Loaded,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Divider()

        state.jetpackSocialConnectionDataList.forEachIndexed { index, data ->
            // divider between items
            if (index != 0) Divider()

            Text("Toggle for ${data.postSocialConnection.externalName}")
        }

        Divider()

        PrimaryButton(
            text = state.subscribeButtonLabel,
            onClick = state.onSubscribeClick,
        )
    }
}
