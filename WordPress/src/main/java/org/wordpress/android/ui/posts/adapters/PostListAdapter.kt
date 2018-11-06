package org.wordpress.android.ui.posts.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.arch.paging.PagedListAdapter
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ProgressBar
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.PagedListItemType
import org.wordpress.android.ui.posts.PagedListItemType.EndListIndicatorItem
import org.wordpress.android.ui.posts.PagedListItemType.LoadingItem
import org.wordpress.android.ui.posts.PagedListItemType.ReadyItem
import org.wordpress.android.ui.posts.PostAdapterItem
import org.wordpress.android.ui.posts.PostAdapterItemData
import org.wordpress.android.ui.posts.PostAdapterItemUploadStatus
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.widgets.PostListButton
import javax.inject.Inject

private const val ROW_ANIM_DURATION: Long = 150
private const val MAX_DISPLAYED_UPLOAD_PROGRESS = 90

private const val VIEW_TYPE_POST = 0
private const val VIEW_TYPE_ENDLIST_INDICATOR = 1
private const val VIEW_TYPE_LOADING = 2

class PostListAdapter(
    context: Context,
    private val isAztecEditorEnabled: Boolean,
    private val hasCapabilityPublishPosts: Boolean,
    private val isPhotonCapable: Boolean
) : PagedListAdapter<PagedListItemType<PostAdapterItem>, ViewHolder>(DiffItemCallback) {
    private val photonWidth: Int
    private val photonHeight: Int
    private val endlistIndicatorHeight: Int

    private val showAllButtons: Boolean

    private val layoutInflater: LayoutInflater

    @Inject internal lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)

        layoutInflater = LayoutInflater.from(context)

        val displayWidth = DisplayUtils.getDisplayPixelWidth(context)
        val contentSpacing = context.resources.getDimensionPixelSize(R.dimen.content_margin)
        photonWidth = displayWidth - contentSpacing * 2
        photonHeight = context.resources.getDimensionPixelSize(R.dimen.reader_featured_image_height)

        // endlist indicator height is hard-coded here so that its horizontal line is in the middle of the fab
        endlistIndicatorHeight = DisplayUtils.dpToPx(context, 74)

        // on larger displays we can always show all buttons
        showAllButtons = displayWidth >= 1080
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EndListIndicatorItem -> VIEW_TYPE_ENDLIST_INDICATOR
            is LoadingItem -> VIEW_TYPE_LOADING
            is ReadyItem<PostAdapterItem> -> VIEW_TYPE_POST
            null -> VIEW_TYPE_LOADING // Placeholder by paged list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ENDLIST_INDICATOR -> {
                val view = layoutInflater.inflate(R.layout.endlist_indicator, parent, false)
                view.layoutParams.height = endlistIndicatorHeight
                EndListViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = layoutInflater.inflate(R.layout.post_cardview_skeleton, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_POST -> {
                val view = layoutInflater.inflate(R.layout.post_cardview, parent, false)
                PostViewHolder(view)
            }
            else -> {
                // Fail fast if a new view type is added so the we can handle it
                throw IllegalStateException("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // nothing to do if this is the static endlist indicator
        if (holder is EndListViewHolder) {
            return
        }
        if (holder is LoadingViewHolder) {
            return
        }
        val postAdapterItemReady = getItem(position)
        if (holder !is PostViewHolder || postAdapterItemReady !is ReadyItem<PostAdapterItem>) {
            // Fail fast if a new view type is added so the we can handle it
            throw IllegalStateException(
                    "Only remaining ViewHolder type should be PostViewHolder and only remaining" +
                            " adapter item type should be PostAdapterItemPost"
            )
        }

        // TODO: Rename things a bit to cleanup
        val context = holder.itemView.context
        val postAdapterItem = postAdapterItemReady.item
        val postData = postAdapterItem.data

        holder.title.text = if (!postData.title.isNullOrBlank()) {
            postData.title
        } else context.getString(R.string.untitled_in_parentheses)

        if (!postData.excerpt.isNullOrBlank()) {
            holder.excerpt.text = postData.excerpt
            holder.excerpt.visibility = View.VISIBLE
        } else {
            holder.excerpt.visibility = View.GONE
        }

        showFeaturedImage(postData.shouldShowFeaturedImage, postData.featuredImageUrl, holder.featuredImage)

        // local drafts say "delete" instead of "trash"
        if (postData.isLocalDraft) {
            holder.date.visibility = View.GONE
            holder.trashButton.buttonType = PostListButton.BUTTON_DELETE
        } else {
            holder.date.text = postData.date
            holder.date.visibility = View.VISIBLE
            holder.trashButton.buttonType = PostListButton.BUTTON_TRASH
        }

        updateForUploadStatus(holder, postData.uploadStatus)
        updateStatusTextAndImage(holder.status, holder.statusImage, postData)
        configurePostButtons(holder, postAdapterItem)
        holder.itemView.setOnClickListener {
            postAdapterItem.onSelected()
        }
    }

    private fun updateForUploadStatus(holder: PostViewHolder, uploadStatus: PostAdapterItemUploadStatus) {
        if (uploadStatus.isUploading) {
            holder.disabledOverlay.visibility = View.VISIBLE
            holder.progressBar.isIndeterminate = true
        } else if (!isAztecEditorEnabled && uploadStatus.isUploadingOrQueued) {
            // TODO: Is this logic correct? Do we need to check for is uploading still?
            // Editing posts with uploading media is only supported in Aztec
            holder.disabledOverlay.visibility = View.VISIBLE
        } else {
            holder.progressBar.isIndeterminate = false
            holder.disabledOverlay.visibility = View.GONE
        }
        if (!uploadStatus.isUploadFailed &&
                (uploadStatus.isUploadingOrQueued || uploadStatus.hasInProgressMediaUpload)) {
            holder.progressBar.visibility = View.VISIBLE
            // Sometimes the progress bar can be stuck at 100% for a long time while further processing happens
            // Cap the progress bar at MAX_DISPLAYED_UPLOAD_PROGRESS (until we move past the 'uploading media' phase)
            holder.progressBar.progress = Math.min(MAX_DISPLAYED_UPLOAD_PROGRESS, uploadStatus.mediaUploadProgress)
        } else {
            holder.progressBar.visibility = View.GONE
        }
    }

    // TODO: Rework this to move everything to ViewModel
    private fun showFeaturedImage(shouldShowFeaturedImage: Boolean, imageUrl: String?, imgFeatured: ImageView) {
        imgFeatured.visibility = if (shouldShowFeaturedImage) View.VISIBLE else View.GONE
        if (imageUrl == null || !shouldShowFeaturedImage) {
            imageManager.cancelRequestAndClearImageView(imgFeatured)
        } else if (imageUrl.startsWith("http")) {
            val photonUrl = ReaderUtils.getResizedImageUrl(
                    imageUrl, photonWidth, photonHeight, !isPhotonCapable
            )
            imageManager.load(imgFeatured, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
        } else {
            val bmp = ImageUtils.getWPImageSpanThumbnailFromFilePath(
                    imgFeatured.context, imageUrl, photonWidth
            )
            if (bmp != null) {
                imageManager.load(imgFeatured, bmp)
            } else {
                // Override the `shouldShowFeaturedImage` since we couldn't load the image
                imgFeatured.visibility = View.GONE
                imageManager.cancelRequestAndClearImageView(imgFeatured)
            }
        }
    }

    private fun updateStatusTextAndImage(
        txtStatus: TextView,
        imgStatus: ImageView,
        postAdapterItem: PostAdapterItemData
    ) {
        val context = txtStatus.context

        if (postAdapterItem.postStatus == PostStatus.PUBLISHED && !postAdapterItem.isLocalDraft &&
                !postAdapterItem.isLocallyChanged) {
            txtStatus.visibility = View.GONE
            imgStatus.visibility = View.GONE
            imageManager.cancelRequestAndClearImageView(imgStatus)
        } else {
            var statusTextResId = 0
            var statusIconResId = 0
            var statusColorResId = R.color.grey_darken_10
            var errorMessage: String? = null

            if (postAdapterItem.uploadStatus.uploadError != null &&
                    !postAdapterItem.uploadStatus.hasInProgressMediaUpload) {
                if (postAdapterItem.uploadStatus.uploadError.mediaError != null) {
                    errorMessage = context.getString(R.string.error_media_recover_post)
                } else if (postAdapterItem.uploadStatus.uploadError.postError != null) {
                    // TODO: figure out!!
//                    errorMessage = UploadUtils.getErrorMessageFromPostError(context, post, reason.postError)
                }
                statusIconResId = R.drawable.ic_gridicons_cloud_upload
                statusColorResId = R.color.alert_red
            } else if (postAdapterItem.uploadStatus.isUploading) {
                statusTextResId = R.string.post_uploading
                statusIconResId = R.drawable.ic_gridicons_cloud_upload
            } else if (postAdapterItem.uploadStatus.hasInProgressMediaUpload) {
                statusTextResId = R.string.uploading_media
                statusIconResId = R.drawable.ic_gridicons_cloud_upload
            } else if (postAdapterItem.uploadStatus.isQueued || postAdapterItem.uploadStatus.hasPendingMediaUpload) {
                // the Post (or its related media if such a thing exist) *is strictly* queued
                statusTextResId = R.string.post_queued
                statusIconResId = R.drawable.ic_gridicons_cloud_upload
            } else if (postAdapterItem.isLocalDraft) {
                statusTextResId = R.string.local_draft
                statusIconResId = R.drawable.ic_gridicons_page
                statusColorResId = R.color.alert_yellow_dark
            } else if (postAdapterItem.isLocallyChanged) {
                statusTextResId = R.string.local_changes
                statusIconResId = R.drawable.ic_gridicons_page
                statusColorResId = R.color.alert_yellow_dark
            } else {
                when (postAdapterItem.postStatus) {
                    PostStatus.DRAFT -> {
                        statusTextResId = R.string.post_status_draft
                        statusIconResId = R.drawable.ic_gridicons_page
                        statusColorResId = R.color.alert_yellow_dark
                    }
                    PostStatus.PRIVATE -> statusTextResId = R.string.post_status_post_private
                    PostStatus.PENDING -> {
                        statusTextResId = R.string.post_status_pending_review
                        statusIconResId = R.drawable.ic_gridicons_page
                        statusColorResId = R.color.alert_yellow_dark
                    }
                    PostStatus.SCHEDULED -> {
                        statusTextResId = R.string.post_status_scheduled
                        statusIconResId = R.drawable.ic_gridicons_calendar
                        statusColorResId = R.color.blue_medium
                    }
                    PostStatus.TRASHED -> {
                        statusTextResId = R.string.post_status_trashed
                        statusIconResId = R.drawable.ic_gridicons_page
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
            txtStatus.setTextColor(resources.getColor(statusColorResId))
            if (!TextUtils.isEmpty(errorMessage)) {
                txtStatus.text = errorMessage
            } else {
                txtStatus.text = if (statusTextResId != 0) resources.getString(statusTextResId) else ""
            }
            txtStatus.visibility = View.VISIBLE

            var drawable: Drawable? = if (statusIconResId != 0) resources.getDrawable(statusIconResId) else null
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable)
                DrawableCompat.setTint(drawable, resources.getColor(statusColorResId))
                imgStatus.visibility = View.VISIBLE
                imageManager.load(imgStatus, drawable)
            } else {
                imgStatus.visibility = View.GONE
                imageManager.cancelRequestAndClearImageView(imgStatus)
            }
        }
    }

    private fun configurePostButtons(
        holder: PostViewHolder,
        postAdapterItem: PostAdapterItem
    ) {
        val postData = postAdapterItem.data
        val canShowViewButton = !postData.canRetryUpload
        val canShowPublishButton = postData.canRetryUpload || postData.canPublishPost

        // publish button is re-purposed depending on the situation
        if (canShowPublishButton) {
            if (!hasCapabilityPublishPosts) {
                holder.publishButton.buttonType = PostListButton.BUTTON_SUBMIT
            } else if (postData.canRetryUpload) {
                holder.publishButton.buttonType = PostListButton.BUTTON_RETRY
            } else if (postData.postStatus == PostStatus.SCHEDULED && postData.isLocallyChanged) {
                holder.publishButton.buttonType = PostListButton.BUTTON_SYNC
            } else {
                holder.publishButton.buttonType = PostListButton.BUTTON_PUBLISH
            }
        }

        // posts with local changes have preview rather than view button
        if (canShowViewButton) {
            if (postData.isLocalDraft || postData.isLocallyChanged) {
                holder.viewButton.buttonType = PostListButton.BUTTON_PREVIEW
            } else {
                holder.viewButton.buttonType = PostListButton.BUTTON_VIEW
            }
        }

        // edit is always visible
        holder.editButton.visibility = View.VISIBLE
        holder.viewButton.visibility = if (canShowViewButton) View.VISIBLE else View.GONE

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
        if (showAllButtons || numVisibleButtons <= 3) {
            holder.moreButton.visibility = View.GONE
            holder.backButton.visibility = View.GONE
            holder.trashButton.visibility = View.VISIBLE
            holder.statsButton.visibility = if (postData.canShowStats) View.VISIBLE else View.GONE
            holder.publishButton.visibility = if (canShowPublishButton) View.VISIBLE else View.GONE
        } else {
            holder.moreButton.visibility = View.VISIBLE
            holder.backButton.visibility = View.GONE
            holder.trashButton.visibility = View.GONE
            holder.statsButton.visibility = View.GONE
            holder.publishButton.visibility = View.GONE
        }

        val btnClickListener = View.OnClickListener { view ->
            // handle back/more here, pass other actions to activity/fragment
            val buttonType = (view as PostListButton).buttonType
            when (buttonType) {
                PostListButton.BUTTON_MORE -> animateButtonRows(holder, postData, false)
                PostListButton.BUTTON_BACK -> animateButtonRows(holder, postData, true)
                else -> postAdapterItem.onButtonClicked(buttonType)
            }
        }
        holder.editButton.setOnClickListener(btnClickListener)
        holder.viewButton.setOnClickListener(btnClickListener)
        holder.statsButton.setOnClickListener(btnClickListener)
        holder.trashButton.setOnClickListener(btnClickListener)
        holder.moreButton.setOnClickListener(btnClickListener)
        holder.backButton.setOnClickListener(btnClickListener)
        holder.publishButton.setOnClickListener(btnClickListener)
    }

    /*
     * buttons may appear in two rows depending on display size and number of visible
     * buttons - these rows are toggled through the "more" and "back" buttons - this
     * routine is used to animate the new row in and the old row out
     */
    private fun animateButtonRows(
        holder: PostViewHolder,
        postAdapterItem: PostAdapterItemData,
        showRow1: Boolean
    ) {
        // first animate out the button row, then show/hide the appropriate buttons,
        // then animate the row layout back in
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0f)
        val animOut = ObjectAnimator.ofPropertyValuesHolder(holder.buttonsLayout, scaleX, scaleY)
        animOut.duration = ROW_ANIM_DURATION
        animOut.interpolator = AccelerateInterpolator()

        animOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // row 1
                holder.editButton.visibility = if (showRow1) View.VISIBLE else View.GONE
                holder.viewButton.visibility = if (showRow1) View.VISIBLE else View.GONE
                holder.moreButton.visibility = if (showRow1) View.VISIBLE else View.GONE
                // row 2
                holder.statsButton.visibility = if (!showRow1 && postAdapterItem.canShowStats) {
                    View.VISIBLE
                } else View.GONE
                holder.publishButton.visibility = if (!showRow1 && postAdapterItem.canPublishPost) {
                    View.VISIBLE
                } else View.GONE
                holder.trashButton.visibility = if (!showRow1) View.VISIBLE else View.GONE
                holder.backButton.visibility = if (!showRow1) View.VISIBLE else View.GONE

                val updatedScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0f, 1f)
                val updatedScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0f, 1f)
                val animIn = ObjectAnimator.ofPropertyValuesHolder(holder.buttonsLayout, updatedScaleX, updatedScaleY)
                animIn.duration = ROW_ANIM_DURATION
                animIn.interpolator = DecelerateInterpolator()
                animIn.start()
            }
        })

        animOut.start()
    }

    private class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.text_title)
        val excerpt: TextView = view.findViewById(R.id.text_excerpt)
        val date: TextView = view.findViewById(R.id.text_date)
        val status: TextView = view.findViewById(R.id.text_status)

        val statusImage: ImageView = view.findViewById(R.id.image_status)
        val featuredImage: ImageView = view.findViewById(R.id.image_featured)

        val editButton: PostListButton = view.findViewById(R.id.btn_edit)
        val viewButton: PostListButton = view.findViewById(R.id.btn_view)
        val publishButton: PostListButton = view.findViewById(R.id.btn_publish)
        val moreButton: PostListButton = view.findViewById(R.id.btn_more)
        val statsButton: PostListButton = view.findViewById(R.id.btn_stats)
        val trashButton: PostListButton = view.findViewById(R.id.btn_trash)
        val backButton: PostListButton = view.findViewById(R.id.btn_back)
        val buttonsLayout: ViewGroup = view.findViewById(R.id.layout_buttons)

        val disabledOverlay: View = view.findViewById(R.id.disabled_overlay)
        val progressBar: ProgressBar = view.findViewById(R.id.post_upload_progress)
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class EndListViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

object DiffItemCallback : DiffUtil.ItemCallback<PagedListItemType<PostAdapterItem>>() {
    override fun areItemsTheSame(
        oldItem: PagedListItemType<PostAdapterItem>,
        newItem: PagedListItemType<PostAdapterItem>
    ): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteItemId == newItem.remoteItemId
        }
        if (oldItem is ReadyItem && newItem is ReadyItem) {
            return oldItem.item.data.localPostId == newItem.item.data.localPostId
        }
        if (oldItem is LoadingItem && newItem is ReadyItem) {
            return oldItem.remoteItemId == newItem.item.data.remotePostId
        }
        if (oldItem is ReadyItem && newItem is LoadingItem) {
            return oldItem.item.data.remotePostId == newItem.remoteItemId
        }
        return false
    }

    override fun areContentsTheSame(
        oldItem: PagedListItemType<PostAdapterItem>,
        newItem: PagedListItemType<PostAdapterItem>
    ): Boolean {
        if (oldItem is EndListIndicatorItem && newItem is EndListIndicatorItem) {
            return true
        }
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteItemId == newItem.remoteItemId
        }
        if (oldItem is ReadyItem && newItem is ReadyItem) {
            return oldItem.item.data == newItem.item.data
        }
        return false
    }
}
