package org.wordpress.android.ui.posts

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState.Loaded
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState.Loading
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.JetpackSocialUiState.NoConnections

class EditPostSettingsJetpackSocialContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    private val uiState: MutableState<JetpackSocialUiState> = mutableStateOf(Loading)

    var jetpackSocialUiState: JetpackSocialUiState
        get() = uiState.value
        set(value) {
            if (uiState.value != value) uiState.value = value
        }

    @Composable
    override fun Content() {
        AppThemeEditor {
            with(uiState.value) {
                when (this) {
                    is Loading -> {
                        /*TODO*/
                    }
                    is Loaded -> {
                        EditPostSettingsJetpackSocialContainer(
                            postSocialConnectionList = postSocialConnectionList,
                            showShareLimitUi = showShareLimitUi,
                            shareMessage = shareMessage,
                            remainingSharesMessage = remainingSharesMessage,
                            subscribeButtonLabel = subscribeButtonLabel,
                            onSubscribeClick = onSubscribeClick,
                        )
                    }
                    is NoConnections -> {
                        EditPostSettingsJetpackSocialNoConnections(
                            trainOfIconsModels = trainOfIconsModels,
                            connectProfilesButtonLabel = connectProfilesButtonLabel,
                            onConnectProfilesCLick = onConnectProfilesClick,
                        )
                    }
                }
            }
        }
    }
}
