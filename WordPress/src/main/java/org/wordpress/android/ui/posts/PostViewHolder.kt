package org.wordpress.android.ui.posts

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ProgressBar
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.widgets.PostListButton

private const val ROW_ANIM_DURATION: Long = 150
private const val MAX_DISPLAYED_UPLOAD_PROGRESS = 90

class PostViewHolderConfig(
    val endlistIndicatorHeight: Int,
    val photonWidth: Int,
    val photonHeight: Int,
    val isPhotonCapable: Boolean,
    val showAllButtons: Boolean,
    val imageManager: ImageManager,
    val isAztecEditorEnabled: Boolean,
    val hasCapabilityPublishPosts: Boolean
)

class PostViewHolder(private val view: View, private val config: PostViewHolderConfig) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.text_title)
    private val excerpt: TextView = view.findViewById(R.id.text_excerpt)
    private val date: TextView = view.findViewById(R.id.text_date)
    private val status: TextView = view.findViewById(R.id.text_status)

    private val statusImage: ImageView = view.findViewById(R.id.image_status)
    private val featuredImage: ImageView = view.findViewById(R.id.image_featured)

    private val editButton: PostListButton = view.findViewById(R.id.btn_edit)
    private val viewButton: PostListButton = view.findViewById(R.id.btn_view)
    private val publishButton: PostListButton = view.findViewById(R.id.btn_publish)
    private val moreButton: PostListButton = view.findViewById(R.id.btn_more)
    private val statsButton: PostListButton = view.findViewById(R.id.btn_stats)
    private val trashButton: PostListButton = view.findViewById(R.id.btn_trash)
    private val backButton: PostListButton = view.findViewById(R.id.btn_back)
    private val buttonsLayout: ViewGroup = view.findViewById(R.id.layout_buttons)

    private val disabledOverlay: View = view.findViewById(R.id.disabled_overlay)
    private val progressBar: ProgressBar = view.findViewById(R.id.post_upload_progress)

    fun onBind(postAdapterItem: PostAdapterItem) {
        val context = view.context
        val postData = postAdapterItem.data
        title.text = if (!postData.title.isNullOrBlank()) {
            postData.title
        } else context.getString(R.string.untitled_in_parentheses)

        if (!postData.excerpt.isNullOrBlank()) {
            excerpt.text = postData.excerpt
            excerpt.visibility = View.VISIBLE
        } else {
            excerpt.visibility = View.GONE
        }

        showFeaturedImage(postData.featuredImageUrl)

        // local drafts say "delete" instead of "trash"
        if (postData.isLocalDraft) {
            date.visibility = View.GONE
            trashButton.buttonType = PostListButton.BUTTON_DELETE
        } else {
            date.text = postData.date
            date.visibility = View.VISIBLE
            trashButton.buttonType = PostListButton.BUTTON_TRASH
        }

        updateForUploadStatus(postData.uploadStatus)
        updateStatusTextAndImage(postData)
        configurePostButtons(postAdapterItem)
        itemView.setOnClickListener {
            postAdapterItem.onSelected()
        }
    }

    private fun updateForUploadStatus(uploadStatus: PostAdapterItemUploadStatus) {
        if (uploadStatus.isUploading) {
            disabledOverlay.visibility = View.VISIBLE
            progressBar.isIndeterminate = true
        } else if (!config.isAztecEditorEnabled && uploadStatus.isUploadingOrQueued) {
            // Editing posts with uploading media is only supported in Aztec
            disabledOverlay.visibility = View.VISIBLE
        } else {
            progressBar.isIndeterminate = false
            disabledOverlay.visibility = View.GONE
        }
        if (!uploadStatus.isUploadFailed &&
                (uploadStatus.isUploadingOrQueued || uploadStatus.hasInProgressMediaUpload)) {
            progressBar.visibility = View.VISIBLE
            // Sometimes the progress bar can be stuck at 100% for a long time while further processing happens
            // Cap the progress bar at MAX_DISPLAYED_UPLOAD_PROGRESS (until we move past the 'uploading media' phase)
            progressBar.progress = Math.min(MAX_DISPLAYED_UPLOAD_PROGRESS, uploadStatus.mediaUploadProgress)
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun showFeaturedImage(imageUrl: String?) {
        if (imageUrl == null) {
            featuredImage.visibility = View.GONE
            config.imageManager.cancelRequestAndClearImageView(featuredImage)
        } else if (imageUrl.startsWith("http")) {
            val photonUrl = ReaderUtils.getResizedImageUrl(
                    imageUrl, config.photonWidth, config.photonHeight, !config.isPhotonCapable
            )
            featuredImage.visibility = View.VISIBLE
            config.imageManager.load(featuredImage, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
        } else {
            val bmp = ImageUtils.getWPImageSpanThumbnailFromFilePath(
                    featuredImage.context, imageUrl, config.photonWidth
            )
            if (bmp != null) {
                featuredImage.visibility = View.VISIBLE
                config.imageManager.load(featuredImage, bmp)
            } else {
                featuredImage.visibility = View.GONE
                config.imageManager.cancelRequestAndClearImageView(featuredImage)
            }
        }
    }

    private fun updateStatusTextAndImage(postAdapterItem: PostAdapterItemData) {
        val context = view.context

        if (postAdapterItem.postStatus == PostStatus.PUBLISHED && !postAdapterItem.isLocalDraft &&
                !postAdapterItem.isLocallyChanged) {
            status.visibility = View.GONE
            statusImage.visibility = View.GONE
            config.imageManager.cancelRequestAndClearImageView(statusImage)
        } else {
            var statusTextResId = 0
            var statusIconResId = 0
            var statusColorResId = R.color.grey_darken_10
            var errorMessage: String? = null
            val uploadError = postAdapterItem.uploadStatus.uploadError

            if (uploadError != null && !postAdapterItem.uploadStatus.hasInProgressMediaUpload) {
                if (uploadError.mediaError != null) {
                    errorMessage = context.getString(R.string.error_media_recover_post)
                } else if (uploadError.postError != null) {
                    errorMessage = UploadUtils.getErrorMessageFromPostError(context, false, uploadError.postError)
                }
                statusIconResId = R.drawable.ic_cloud_upload_white_24dp
                statusColorResId = R.color.alert_red
            } else if (postAdapterItem.uploadStatus.isUploading) {
                statusTextResId = R.string.post_uploading
                statusIconResId = R.drawable.ic_cloud_upload_white_24dp
            } else if (postAdapterItem.uploadStatus.hasInProgressMediaUpload) {
                statusTextResId = R.string.uploading_media
                statusIconResId = R.drawable.ic_cloud_upload_white_24dp
            } else if (postAdapterItem.uploadStatus.isQueued || postAdapterItem.uploadStatus.hasPendingMediaUpload) {
                // the Post (or its related media if such a thing exist) *is strictly* queued
                statusTextResId = R.string.post_queued
                statusIconResId = R.drawable.ic_cloud_upload_white_24dp
            } else if (postAdapterItem.isConflicted) {
                statusTextResId = R.string.local_post_is_conflicted
                statusIconResId = R.drawable.ic_notice_white_24dp
                statusColorResId = R.color.alert_red
            } else if (postAdapterItem.isLocalDraft) {
                statusTextResId = R.string.local_draft
                statusIconResId = R.drawable.ic_pages_white_24dp
                statusColorResId = R.color.alert_yellow_dark
            } else if (postAdapterItem.isLocallyChanged) {
                statusTextResId = R.string.local_changes
                statusIconResId = R.drawable.ic_pages_white_24dp
                statusColorResId = R.color.alert_yellow_dark
            } else {
                when (postAdapterItem.postStatus) {
                    PostStatus.DRAFT -> {
                        statusTextResId = R.string.post_status_draft
                        statusIconResId = R.drawable.ic_pages_white_24dp
                        statusColorResId = R.color.alert_yellow_dark
                    }
                    PostStatus.PRIVATE -> statusTextResId = R.string.post_status_post_private
                    PostStatus.PENDING -> {
                        statusTextResId = R.string.post_status_pending_review
                        statusIconResId = R.drawable.ic_pages_white_24dp
                        statusColorResId = R.color.alert_yellow_dark
                    }
                    PostStatus.SCHEDULED -> {
                        statusTextResId = R.string.post_status_scheduled
                        statusIconResId = R.drawable.ic_calendar_white_24dp
                        statusColorResId = R.color.blue_medium
                    }
                    PostStatus.TRASHED -> {
                        statusTextResId = R.string.post_status_trashed
                        statusIconResId = R.drawable.ic_pages_white_24dp
                        statusColorResId = R.color.alert_red
                    }
                    PostStatus.UNKNOWN -> {
                    }
                    PostStatus.PUBLISHED -> {
                    }
                    else ->
                        // no-op
                        return
                }
            }

            val resources = context.resources
            status.setTextColor(resources.getColor(statusColorResId))
            if (!TextUtils.isEmpty(errorMessage)) {
                status.text = errorMessage
            } else {
                status.text = if (statusTextResId != 0) resources.getString(statusTextResId) else ""
            }
            status.visibility = View.VISIBLE

            var drawable: Drawable? = if (statusIconResId != 0) resources.getDrawable(statusIconResId) else null
            if (drawable != null) {
                drawable = ColorUtils.applyTintToDrawable(context, statusIconResId, statusColorResId)
                statusImage.visibility = View.VISIBLE
                config.imageManager.load(statusImage, drawable)
            } else {
                statusImage.visibility = View.GONE
                config.imageManager.cancelRequestAndClearImageView(statusImage)
            }
        }
    }

    private fun configurePostButtons(postAdapterItem: PostAdapterItem) {
        val postData = postAdapterItem.data
        val canShowViewButton = !postData.canRetryUpload
        val canShowPublishButton = postData.canRetryUpload || postData.canPublishPost

        // publish button is re-purposed depending on the situation
        if (canShowPublishButton) {
            if (!config.hasCapabilityPublishPosts) {
                publishButton.buttonType = PostListButton.BUTTON_SUBMIT
            } else if (postData.canRetryUpload) {
                publishButton.buttonType = PostListButton.BUTTON_RETRY
            } else if (postData.postStatus == PostStatus.SCHEDULED && postData.isLocallyChanged) {
                publishButton.buttonType = PostListButton.BUTTON_SYNC
            } else {
                publishButton.buttonType = PostListButton.BUTTON_PUBLISH
            }
        }

        // posts with local changes have preview rather than view button
        if (canShowViewButton) {
            if (postData.isLocalDraft || postData.isLocallyChanged) {
                viewButton.buttonType = PostListButton.BUTTON_PREVIEW
            } else {
                viewButton.buttonType = PostListButton.BUTTON_VIEW
            }
        }

        // edit is always visible
        editButton.visibility = View.VISIBLE
        viewButton.visibility = if (canShowViewButton) View.VISIBLE else View.GONE

        var numVisibleButtons = 2
        if (canShowViewButton) {
            numVisibleButtons++
        }
        if (canShowPublishButton) {
            numVisibleButtons++
        }
        if (postData.canShowStats) {
            numVisibleButtons++
        }

        // if there's enough room to show all buttons then hide back/more and show stats/trash/publish,
        // otherwise show the more button and hide stats/trash/publish
        if (config.showAllButtons || numVisibleButtons <= 3) {
            moreButton.visibility = View.GONE
            backButton.visibility = View.GONE
            trashButton.visibility = View.VISIBLE
            statsButton.visibility = if (postData.canShowStats) View.VISIBLE else View.GONE
            publishButton.visibility = if (canShowPublishButton) View.VISIBLE else View.GONE
        } else {
            moreButton.visibility = View.VISIBLE
            backButton.visibility = View.GONE
            trashButton.visibility = View.GONE
            statsButton.visibility = View.GONE
            publishButton.visibility = View.GONE
        }

        val btnClickListener = View.OnClickListener { view ->
            // handle back/more here, pass other actions to activity/fragment
            val buttonType = (view as PostListButton).buttonType
            when (buttonType) {
                PostListButton.BUTTON_MORE -> animateButtonRows(postData, false)
                PostListButton.BUTTON_BACK -> animateButtonRows(postData, true)
            }
            postAdapterItem.onButtonClicked(buttonType)
        }
        editButton.setOnClickListener(btnClickListener)
        viewButton.setOnClickListener(btnClickListener)
        statsButton.setOnClickListener(btnClickListener)
        trashButton.setOnClickListener(btnClickListener)
        moreButton.setOnClickListener(btnClickListener)
        backButton.setOnClickListener(btnClickListener)
        publishButton.setOnClickListener(btnClickListener)
    }

    /*
     * buttons may appear in two rows depending on display size and number of visible
     * buttons - these rows are toggled through the "more" and "back" buttons - this
     * routine is used to animate the new row in and the old row out
     */
    private fun animateButtonRows(postAdapterItem: PostAdapterItemData, showRow1: Boolean) {
        // first animate out the button row, then show/hide the appropriate buttons,
        // then animate the row layout back in
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)
        val animOut = ObjectAnimator.ofPropertyValuesHolder(buttonsLayout, scaleX, scaleY)
        animOut.duration = ROW_ANIM_DURATION
        animOut.interpolator = AccelerateInterpolator()

        animOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // row 1
                editButton.visibility = if (showRow1) View.VISIBLE else View.GONE
                viewButton.visibility = if (showRow1) View.VISIBLE else View.GONE
                moreButton.visibility = if (showRow1) View.VISIBLE else View.GONE
                // row 2
                statsButton.visibility = if (!showRow1 && postAdapterItem.canShowStats) {
                    View.VISIBLE
                } else View.GONE
                publishButton.visibility = if (!showRow1 && postAdapterItem.canPublishPost) {
                    View.VISIBLE
                } else View.GONE
                trashButton.visibility = if (!showRow1) View.VISIBLE else View.GONE
                backButton.visibility = if (!showRow1) View.VISIBLE else View.GONE

                val updatedScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f)
                val updatedScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)
                val animIn = ObjectAnimator.ofPropertyValuesHolder(buttonsLayout, updatedScaleX, updatedScaleY)
                animIn.duration = ROW_ANIM_DURATION
                animIn.interpolator = DecelerateInterpolator()
                animIn.start()
            }
        })

        animOut.start()
    }
}
