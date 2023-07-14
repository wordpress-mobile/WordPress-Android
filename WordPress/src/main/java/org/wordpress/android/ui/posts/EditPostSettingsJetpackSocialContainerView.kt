package org.wordpress.android.ui.posts

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.theme.AppThemeEditor
import org.wordpress.android.ui.posts.social.PostSocialConnection

class EditPostSettingsJetpackSocialContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {
    private var data: EditPostSettingsJetpackSocialContainerComposeViewData? = null

    @Composable
    override fun Content() {
        AppThemeEditor {
            data?.let {
                EditPostSettingsJetpackSocialContainer(
                    trainOfIconsModels = it.trainOfIconsModels,
                    postSocialConnectionList = it.postSocialConnectionList,
                    shareMessage = it.shareMessage,
                    remainingSharesMessage = it.remainingSharesMessage,
                    subscribeButtonLabel = it.subscribeButtonLabel,
                    onSubscribeClick = it.onSubscribeClick,
                    connectProfilesButtonLabel = it.connectProfilesButtonLabel,
                    onConnectProfilesCLick = it.onConnectProfilesCLick,
                )
            }
        }
    }

    fun update(data: EditPostSettingsJetpackSocialContainerComposeViewData) {
        this.data = data
        disposeComposition()
    }
}

data class EditPostSettingsJetpackSocialContainerComposeViewData(
    val trainOfIconsModels: List<TrainOfIconsModel>,
    val postSocialConnectionList: List<PostSocialConnection>,
    val shareMessage: String,
    val remainingSharesMessage: String,
    val subscribeButtonLabel: String,
    val onSubscribeClick: () -> Unit,
    val connectProfilesButtonLabel: String,
    val onConnectProfilesCLick: () -> Unit,
)
