package org.wordpress.android.ui.posts

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.StoryTitleUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.image.ImageType.BLAVATAR

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

            actionType.setTextColor(ContextCompat.getColor(itemView.context, uiState.actionTypeColor))
            actionResult.setTextColor(ContextCompat.getColor(itemView.context, uiState.actionResultColor))
        }
    }

    class PrepublishingStoryTitleItemViewHolder(
        parentView: ViewGroup,
        val uiHelpers: UiHelpers,
        val imageManager: ImageManager
    ) : PrepublishingHomeViewHolder(parentView, R.layout.prepublishing_story_title_list_item) {
        private val storyTitle: TextView = itemView.findViewById(R.id.story_title)
        private val thumbnail: ImageView = itemView.findViewById(R.id.story_thumbnail)

        private val thumbnailCornerRadius = parentView.context.resources.getDimension(R.dimen.prepublishing_site_blavatar_corner_radius)
                .toInt()

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            uiState as StoryTitleUiState

            imageManager.loadImageWithCorners(
                    thumbnail,
                    ImageType.IMAGE,
                    uiState.storyThumbnailUrl,
                    thumbnailCornerRadius
            )

            uiState.storyTitle?.let { storyTitle ->
                uiHelpers.getTextOfUiString(parent.context, storyTitle)
            }
            storyTitle.requestFocus()
            ActivityUtils.showKeyboard(storyTitle)
            storyTitle.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(view: Editable?) {
                    uiState.onStoryTitleChanged.invoke(view.toString())
                }
            })
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

            imageManager.load(siteIcon, BLAVATAR, uiState.siteIconUrl)
        }
    }

    class PrepublishingSubmitButtonViewHolder(parentView: ViewGroup, val uiHelpers: UiHelpers) :
            PrepublishingHomeViewHolder(parentView, R.layout.prepublishing_home_publish_button_list_item) {
        private val button: Button = itemView.findViewById(R.id.publish_button)

        override fun onBind(uiState: PrepublishingHomeItemUiState) {
            uiState as ButtonUiState

            button.text = uiHelpers.getTextOfUiString(itemView.context, uiState.buttonText)
            button.setOnClickListener {
                uiState.onButtonClicked?.invoke(uiState.publishPost)
            }
        }
    }
}
