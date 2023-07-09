package org.wordpress.android.ui.posts.prepublishing.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.asString
import org.wordpress.android.ui.compose.utils.withBottomSheetElevation
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.SocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.compose.PrepublishingHomeSocialItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

sealed class PrepublishingHomeViewHolder(
    internal val parent: ViewGroup,
    @LayoutRes layout: Int
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: PrepublishingHomeItemUiState)

    class PrepublishingHomeListItemViewHolder(parentView: ViewGroup, val uiHelpers: UiHelpers) :
        PrepublishingHomeViewHolder(parentView, R.layout.prepublishing_action_list_item) {
        private val actionType: TextView = itemView.findViewById(R.id.action_type)
        private val actionResult: TextView = itemView.findViewById(R.id.action_result)
        private val actionLayout: View = itemView.findViewById(R.id.action_layout)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            uiState as HomeUiState

            actionType.text = uiHelpers.getTextOfUiString(itemView.context, uiState.actionType.textRes)
            uiState.actionResult?.let { resultText ->
                actionResult.text = uiHelpers.getTextOfUiString(itemView.context, resultText)
            }

            actionLayout.isEnabled = uiState.actionClickable
            actionLayout.setOnClickListener {
                uiState.onActionClicked?.invoke(uiState.actionType)
            }

            actionType.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    uiState.actionTypeColor
                )
            )
            actionResult.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    uiState.actionResultColor
                )
            )
        }
    }

    class PrepublishingHeaderListItemViewHolder(
        parentView: ViewGroup,
        val uiHelpers: UiHelpers,
        val imageManager: ImageManager
    ) : PrepublishingHomeViewHolder(parentView, R.layout.prepublishing_home_header_list_item) {
        private val siteName: TextView = itemView.findViewById(R.id.site_name)
        private val siteIcon: ImageView = itemView.findViewById(R.id.site_icon)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            uiState as HeaderUiState

            siteName.text = uiHelpers.getTextOfUiString(itemView.context, uiState.siteName)

            imageManager.load(siteIcon, ImageType.BLAVATAR, uiState.siteIconUrl)
        }
    }

    class PrepublishingSubmitButtonViewHolder(parentView: ViewGroup, val uiHelpers: UiHelpers) :
        PrepublishingHomeViewHolder(
            parentView,
            R.layout.prepublishing_home_publish_button_list_item
        ) {
        private val button: Button = itemView.findViewById(R.id.publish_button)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            uiState as ButtonUiState

            button.text = uiHelpers.getTextOfUiString(itemView.context, uiState.buttonText)
            button.setOnClickListener {
                uiState.onButtonClicked?.invoke(uiState.publishPost)
            }
        }
    }

    class PrepublishingSocialItemViewHolder(
        parentView: ViewGroup,
        val uiHelpers: UiHelpers,
    ) : PrepublishingHomeViewHolder(parentView, R.layout.prepublishing_home_compose_item) {
        private val composeView: ComposeView = itemView.findViewById(R.id.prepublishing_compose_view)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            composeView.setContent {
                val state: SocialUiState by remember(uiState) {
                    mutableStateOf(uiState as SocialUiState)
                }

                val avatarModels = state.connectionIcons.map { icon ->
                    TrainOfIconsModel(
                        data = icon.iconRes,
                        alpha = if (icon.isEnabled) 1f else ContentAlpha.disabled,
                    )
                }

                AppTheme {
                    PrepublishingHomeSocialItem(
                        title = state.title.asString(),
                        description = state.description.asString(),
                        avatarModels = avatarModels,
                        isLowOnShares = state.isLowOnShares,
                        backgroundColor = MaterialTheme.colors.surface.withBottomSheetElevation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
