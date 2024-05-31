package org.wordpress.android.ui.notifications.blocks

import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Spannable
import android.text.TextUtils
import android.text.style.TypefaceSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.VideoView
import org.wordpress.android.R
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.AccessibilityUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.getMediaOrNull
import org.wordpress.android.util.getMetaIdsSiteIdOrZero
import org.wordpress.android.util.getMetaLinksHomeOrEmpty
import org.wordpress.android.util.getMetaTitlesHomeOrEmpty
import org.wordpress.android.util.getMobileButtonRange
import org.wordpress.android.util.image.GlidePopTransitionOptions.pop
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.util.isMobileButton
import org.wordpress.android.widgets.WPTextView

/**
 * A block of data displayed in a notification.
 * This basic block can support a media item (image/video) and/or text.
 */
open class NoteBlock(
    val noteData: FormattableContent,
    @JvmField protected val mImageManager: ImageManager,
    @JvmField protected val mNotificationsUtilsWrapper: NotificationsUtilsWrapper,
    private val mOnNoteBlockTextClickListener: OnNoteBlockTextClickListener?
) {
    protected open var mIsBadge = false
    private var isPingBack = false
    protected open var mIsViewMilestone = false

    interface OnNoteBlockTextClickListener {
        fun onNoteBlockTextClicked(clickedSpan: NoteBlockClickableSpan?)
        fun showDetailForNoteIds()
        fun showReaderPostComments()
        fun showSitePreview(siteId: Long, siteUrl: String?)
        fun showActionPopup(view: View)
    }

    open val layoutResourceId: Int
        get() = R.layout.note_block_basic
    val onNoteBlockTextClickListener: OnNoteBlockTextClickListener?
        get() = mOnNoteBlockTextClickListener
    open val blockType: BlockType?
        get() = BlockType.BASIC
    open val noteText: Spannable
        get() = mNotificationsUtilsWrapper.getSpannableContentForRanges(
            noteData,
            null,
            mOnNoteBlockTextClickListener,
            false
        )
    val metaHomeTitle: String
        get() = noteData.getMetaTitlesHomeOrEmpty()
    val metaSiteId: Long
        get() = noteData.getMetaIdsSiteIdOrZero()
    open val metaSiteUrl: String?
        get() = noteData.getMetaLinksHomeOrEmpty()

    val noteMediaItem: FormattableMedia?
        get() = noteData.getMediaOrNull(0)

    fun setIsPingback() {
        isPingBack = true
    }

    fun setIsBadge() {
        mIsBadge = true
    }

    fun setIsViewMilestone() {
        mIsViewMilestone = true
    }

    private fun hasMediaArray() = noteData.media != null && noteData.media?.isNotEmpty() == true

    open fun hasImageMediaItem(): Boolean {
        return (hasMediaArray() && noteMediaItem != null && !TextUtils.isEmpty(noteMediaItem?.type)
                && (noteMediaItem?.type?.startsWith("image") == true || noteMediaItem?.type == "badge")
                && !TextUtils.isEmpty(noteMediaItem?.url))
    }

    private fun hasVideoMediaItem(): Boolean {
        return (hasMediaArray() && noteMediaItem != null && !TextUtils.isEmpty(noteMediaItem?.type)
                && noteMediaItem?.type?.startsWith("video") == true
                && !TextUtils.isEmpty(noteMediaItem?.url))
    }

    fun containsBadgeMediaType(): Boolean {
        noteData.media?.forEach {
            if ("badge" == it.type) {
                return true
            }
        }
        return false
    }

    open fun configureView(view: View): View {
        val noteBlockHolder = view.tag as BasicNoteBlockHolder

        // Note image
        if (hasImageMediaItem()) {
            noteBlockHolder.imageView.visibility = View.VISIBLE
            // Request image, and animate it when loaded
            mImageManager.animateWithResultListener(
                noteBlockHolder.imageView, ImageType.IMAGE,
                noteMediaItem?.url ?: "",
                pop(),
                object : ImageManager.RequestListener<Drawable> {
                    override fun onLoadFailed(e: Exception?, model: Any?) {
                        noteBlockHolder.hideImageView()
                    }

                    override fun onResourceReady(resource: Drawable, model: Any?) { /* no-op */
                    }
                })
            if (mIsBadge) {
                noteBlockHolder.imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        } else {
            mImageManager.cancelRequestAndClearImageView(noteBlockHolder.imageView)
            noteBlockHolder.hideImageView()
        }

        // Note video
        if (hasVideoMediaItem()) {
            noteBlockHolder.videoView.setVideoURI(Uri.parse(noteMediaItem?.url ?: ""))
            noteBlockHolder.videoView.visibility = View.VISIBLE
        } else {
            noteBlockHolder.hideVideoView()
        }

        // Note text
        val noteText = noteText
        if (!TextUtils.isEmpty(noteText)) {
            if (isPingBack) {
                noteBlockHolder.textView.visibility = View.GONE
                noteBlockHolder.materialButton.visibility = View.GONE
                noteBlockHolder.divider.visibility = View.VISIBLE
                noteBlockHolder.button.visibility = View.VISIBLE
                noteBlockHolder.button.text = noteText.toString()
                noteBlockHolder.button.setOnClickListener { _: View? ->
                    mOnNoteBlockTextClickListener?.showSitePreview(0, metaSiteUrl)
                }
            } else {
                var textViewVisibility = View.VISIBLE
                if (mIsBadge) {
                    textViewVisibility = handleBadge(noteBlockHolder, view, textViewVisibility, noteText)
                } else {
                    noteBlockHolder.textView.gravity = Gravity.NO_GRAVITY
                    noteBlockHolder.textView.setPadding(0, 0, 0, 0)
                }
                val spans = noteText.getSpans(0, noteText.length, NoteBlockClickableSpan::class.java)
                for (span in spans) {
                    span.enableColors(view.context)
                }
                noteBlockHolder.textView.text = noteText
                noteBlockHolder.textView.visibility = textViewVisibility
            }
        } else {
            noteBlockHolder.button.visibility = View.GONE
            noteBlockHolder.divider.visibility = View.GONE
            noteBlockHolder.materialButton.visibility = View.GONE
            noteBlockHolder.textView.visibility = View.GONE
        }
        return view
    }

    @Suppress("MagicNumber")
    private fun handleBadge(
        noteBlockHolder: BasicNoteBlockHolder,
        view: View,
        textViewVisibility: Int,
        noteText: Spannable
    ): Int {
        var textViewVisibility1 = textViewVisibility
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        params.gravity = Gravity.CENTER_HORIZONTAL
        noteBlockHolder.textView.layoutParams = params
        noteBlockHolder.textView.gravity = Gravity.CENTER_HORIZONTAL
        val padding: Int = if (mIsViewMilestone) 40 else 8
        noteBlockHolder.textView.setPadding(0, DisplayUtils.dpToPx(view.context, padding), 0, 0)
        if (AccessibilityUtils.isAccessibilityEnabled(noteBlockHolder.textView.context)) {
            noteBlockHolder.textView.isClickable = false
            noteBlockHolder.textView.isLongClickable = false
        }
        if (mIsViewMilestone) {
            if (noteData.isMobileButton()) {
                textViewVisibility1 = View.GONE
                noteBlockHolder.button.visibility = View.GONE
                noteBlockHolder.materialButton.visibility = View.VISIBLE
                noteBlockHolder.materialButton.text = noteText.toString()
                noteBlockHolder.materialButton.setOnClickListener { _: View? ->
                    val buttonRange = noteData.getMobileButtonRange()
                    if (buttonRange != null) {
                        val clickableSpan = NoteBlockClickableSpan(buttonRange, true, false)
                        mOnNoteBlockTextClickListener?.onNoteBlockTextClicked(clickableSpan)
                    }
                }
            } else {
                noteBlockHolder.textView.textSize = 28f
                val typefaceSpan = TypefaceSpan("sans-serif")
                noteText.setSpan(typefaceSpan, 0, noteText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return textViewVisibility1
    }

    open fun getViewHolder(view: View): Any = BasicNoteBlockHolder(view)

    internal class BasicNoteBlockHolder(view: View) {
        private val mRootLayout: LinearLayout = view as LinearLayout
        val textView: WPTextView = view.findViewById<WPTextView?>(R.id.note_text).apply {
            movementMethod = NoteBlockLinkMovementMethod()
        }
        val button: Button = view.findViewById(R.id.note_button)
        val materialButton: Button = view.findViewById(R.id.note_material_button)
        val divider: View = view.findViewById(R.id.divider_view)

        val imageView: ImageView by lazy {
            mRootLayout.findViewById(R.id.image)
        }

        @Suppress("MagicNumber")
        val videoView: VideoView by lazy {
            VideoView(mRootLayout.context).apply {
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    DisplayUtils.dpToPx(mRootLayout.context, 220)
                )
                this.layoutParams = layoutParams
                mRootLayout.addView(this, 0)

                // Attach a mediaController if we are displaying a video.
                val mediaController = MediaController(mRootLayout.context)
                mediaController.setMediaPlayer(this)
                this.setMediaController(mediaController)
                mediaController.requestFocus()
                this.setOnPreparedListener { // Show the media controls when the video is ready to be played.
                    mediaController.show(0)
                }
            }
        }

        fun hideImageView() {
            imageView.visibility = View.GONE
        }

        fun hideVideoView() {
            videoView.visibility = View.GONE
        }
    }
}
