package org.wordpress.android.ui.posts.prepublishing.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.JetpackSocialUiState
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.prepublishing.home.PrepublishingHomeItemUiState.FeaturedImageUiState
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

                AppThemeM3 {
                    (state as? SocialUiState.Visible)?.let { visibleState ->
                        when (val internalState = visibleState.state) {
                            is JetpackSocialUiState.Loaded -> {
                                PostSocialSharingItem(
                                    model = internalState.socialSharingModel,
                                    backgroundColor = MaterialTheme.colorScheme.surface,
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
                                    backgroundColor = MaterialTheme.colorScheme.surface,
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

    class PrepublishingFeaturedImageViewHolder(
        parentView: ViewGroup,
        val uiHelpers: UiHelpers,
        val imageManager: ImageManager
    ) : PrepublishingHomeViewHolder(parentView, R.layout.prepublishing_home_featured_image_item) {
        private val card: View = itemView.findViewById(R.id.featured_image_card)
        private val placeholder: View = itemView.findViewById(R.id.featured_image_placeholder)
        private val remoteImage: ImageView = itemView.findViewById(R.id.post_featured_image)
        private val localImage: ImageView = itemView.findViewById(R.id.post_featured_image_local)
        private val retryOverlay: View = itemView.findViewById(R.id.featured_image_retry_overlay)
        private val progressOverlay: View = itemView.findViewById(R.id.featured_image_progress_overlay)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            // Hidden items are filtered out before reaching the adapter, but guard rather than crash
            // if one ever does.
            val uiState = uiState as? FeaturedImageUiState.Visible ?: return
            val state = uiState.featuredImageData.uiState
            // A remote image (on the server) uses one ImageView, a local upload another, so an empty
            // view can never cover a loaded image in the FrameLayout.
            val isRemote = state == FeaturedImageState.REMOTE_IMAGE_LOADING ||
                    state == FeaturedImageState.REMOTE_IMAGE_SET
            val isLocalUpload = state == FeaturedImageState.IMAGE_UPLOAD_IN_PROGRESS ||
                    state == FeaturedImageState.IMAGE_UPLOAD_FAILED
            val isEmpty = state == FeaturedImageState.IMAGE_EMPTY

            with(uiHelpers) {
                updateVisibility(placeholder, isEmpty)
                updateVisibility(remoteImage, isRemote)
                updateVisibility(localImage, isLocalUpload)
                updateVisibility(retryOverlay, state.retryOverlayVisible)
                updateVisibility(progressOverlay, state.progressOverlayVisible)
            }

            val mediaUri = uiState.featuredImageData.mediaUri
            when {
                isRemote && !mediaUri.isNullOrEmpty() -> {
                    imageManager.cancelRequestAndClearImageView(localImage)
                    imageManager.load(remoteImage, ImageType.IMAGE, mediaUri, ScaleType.CENTER_CROP)
                }
                isLocalUpload && !mediaUri.isNullOrEmpty() ->
                    imageManager.load(localImage, ImageType.IMAGE, mediaUri, ScaleType.CENTER_CROP)
                else -> {
                    imageManager.cancelRequestAndClearImageView(remoteImage)
                    imageManager.cancelRequestAndClearImageView(localImage)
                }
            }

            card.setOnClickListener { anchor ->
                when {
                    isEmpty -> uiState.onSetOrReplaceClicked()
                    state == FeaturedImageState.IMAGE_UPLOAD_FAILED -> showOptionsMenu(anchor, uiState, retry = true)
                    else -> showOptionsMenu(anchor, uiState, retry = false)
                }
            }
        }

        private fun showOptionsMenu(anchor: View, uiState: FeaturedImageUiState.Visible, retry: Boolean) {
            PopupMenu(anchor.context, anchor).apply {
                if (retry) {
                    menu.add(0, MENU_RETRY, 0, R.string.post_settings_retry_featured_image)
                } else {
                    menu.add(0, MENU_REPLACE, 0, R.string.post_settings_choose_featured_image)
                }
                menu.add(0, MENU_REMOVE, 1, R.string.post_settings_remove_featured_image)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        MENU_REPLACE -> uiState.onSetOrReplaceClicked()
                        MENU_RETRY -> uiState.onRetryClicked()
                        MENU_REMOVE -> uiState.onRemoveClicked()
                    }
                    true
                }
                show()
            }
        }

        companion object {
            private const val MENU_REPLACE = 1
            private const val MENU_REMOVE = 2
            private const val MENU_RETRY = 3
        }
    }
}
