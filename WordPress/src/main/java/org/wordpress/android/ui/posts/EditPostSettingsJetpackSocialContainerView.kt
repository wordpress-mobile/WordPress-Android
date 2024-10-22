package org.wordpress.android.ui.posts

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import org.wordpress.android.ui.compose.theme.AppThemeM2Editor
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.Loaded
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.Loading
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState.NoConnections
import org.wordpress.android.usecase.social.JetpackSocialFlow

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
        AppThemeM2Editor {
            with(uiState.value) {
                when (this) {
                    is Loading -> {
                        // no-op
                    }

                    is Loaded -> {
                        Column {
                            EditPostJetpackSocialConnectionsContainer(
                                jetpackSocialConnectionDataList = jetpackSocialConnectionDataList,
                                jetpackSocialFlow = JetpackSocialFlow.POST_SETTINGS,
                                shareMessage = shareMessage,
                                isShareMessageEnabled = isShareMessageEnabled,
                                onShareMessageClick = onShareMessageClick,
                            )
                            if (showShareLimitUi) {
                                EditPostSettingsJetpackSocialSharesContainer(
                                    postSocialSharingModel = socialSharingModel,
                                    subscribeButtonLabel = subscribeButtonLabel,
                                    onSubscribeClick = { onSubscribeClick(JetpackSocialFlow.POST_SETTINGS) },
                                )
                            }
                        }
                    }

                    is NoConnections -> {
                        EditPostSettingsJetpackSocialNoConnections(
                            trainOfIconsModels = trainOfIconsModels,
                            message = message,
                            connectProfilesButtonLabel = connectProfilesButtonLabel,
                            onConnectProfilesCLick = { onConnectProfilesClick(JetpackSocialFlow.POST_SETTINGS) },
                            notNowButtonLabel = notNowButtonLabel,
                            onNotNowClick = { onNotNowClick(JetpackSocialFlow.POST_SETTINGS) },
                        )
                    }
                }
            }
        }
    }
}
