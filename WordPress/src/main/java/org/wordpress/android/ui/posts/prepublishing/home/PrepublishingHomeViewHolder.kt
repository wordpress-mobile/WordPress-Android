package org.wordpress.android.ui.posts.prepublishing.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.withBottomSheetElevation
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.SocialUiState
import org.wordpress.android.ui.posts.prepublishing.home.compose.PrepublishingHomeSocialNoConnectionsItem
import org.wordpress.android.ui.posts.social.compose.PostSocialSharingItem
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.usecase.social.JetpackSocialFlow
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

            actionType.text = uiHelpers.getTextOfUiString(itemView.context, uiState.navigationAction.textRes)
            uiState.actionResult?.let { resultText ->
                actionResult.text = uiHelpers.getTextOfUiString(itemView.context, resultText)
            }

            actionLayout.isEnabled = uiState.actionClickable
            actionLayout.setOnClickListener {
                uiState.onNavigationActionClicked?.invoke(uiState.navigationAction)
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
        private val dividerView: View = itemView.findViewById(R.id.bottom_divider)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            require(uiState is SocialUiState) {
                "PrepublishingSocialItemViewHolder can only bind SocialUiState"
            }

            dividerView.isGone = uiState is SocialUiState.Visible &&
                    uiState.state is JetpackSocialUiState.NoConnections

            composeView.setContent {
                val state: SocialUiState by remember(uiState) {
                    mutableStateOf(uiState)
                }

                AppThemeM2 {
                    (state as? SocialUiState.Visible)?.let { visibleState ->
                        when (val internalState = visibleState.state) {
                            is JetpackSocialUiState.Loaded -> {
                                PostSocialSharingItem(
                                    model = internalState.socialSharingModel,
                                    backgroundColor = MaterialTheme.colors.surface.withBottomSheetElevation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null, // no ripple
                                            onClick = visibleState.onItemClicked,
                                        ),
                                )
                            }

                            is JetpackSocialUiState.NoConnections -> {
                                PrepublishingHomeSocialNoConnectionsItem(
                                    connectionIconModels = internalState.trainOfIconsModels,
                                    onConnectClick = {
                                        internalState.onConnectProfilesClick(JetpackSocialFlow.PRE_PUBLISHING)
                                     },
                                    onDismissClick = { internalState.onNotNowClick(JetpackSocialFlow.PRE_PUBLISHING) },
                                    backgroundColor = MaterialTheme.colors.surface.withBottomSheetElevation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            JetpackSocialUiState.Loading -> {} // do nothing
                        }
                    }
                }
            }
        }
    }
}
