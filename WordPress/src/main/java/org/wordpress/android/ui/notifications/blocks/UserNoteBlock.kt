package org.wordpress.android.ui.notifications.blocks

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.WPAvatarUtils
import org.wordpress.android.util.getMetaIdsSiteIdOrZero
import org.wordpress.android.util.getMetaIdsUserIdOrZero
import org.wordpress.android.util.getMetaLinksHomeOrEmpty
import org.wordpress.android.util.getMetaTitlesHomeOrEmpty
import org.wordpress.android.util.getMetaTitlesTaglineOrEmpty
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType

/**
 * A block that displays information about a User (such as a user that liked a post)
 */
open class UserNoteBlock(
    context: Context?,
    noteObject: FormattableContent,
    onNoteBlockTextClickListener: OnNoteBlockTextClickListener?,
    onGravatarClickedListener: OnGravatarClickedListener?,
    imageManager: ImageManager,
    notificationsUtilsWrapper: NotificationsUtilsWrapper
) : NoteBlock(noteObject, imageManager, notificationsUtilsWrapper, onNoteBlockTextClickListener) {
    private val mGravatarClickedListener: OnGravatarClickedListener?
    protected val avatarSize: Int

    override val blockType: BlockType
        get() = BlockType.USER
    override val layoutResourceId: Int
        get() = R.layout.note_block_user

    val userUrl: String
        get() = noteData.getMetaLinksHomeOrEmpty()
    private val userBlogTitle: String
        get() = noteData.getMetaTitlesHomeOrEmpty()
    private val userBlogTagline: String
        get() = noteData.getMetaTitlesTaglineOrEmpty()

    private val mOnClickListener = View.OnClickListener { showBlogPreview() }

    init {
        avatarSize = context?.resources?.getDimensionPixelSize(R.dimen.notifications_avatar_sz) ?: 0
        mGravatarClickedListener = onGravatarClickedListener
    }

    interface OnGravatarClickedListener {
        // userId is currently unused, but will be handy once a profile view is added to the app
        fun onGravatarClicked(siteId: Long, userId: Long, siteUrl: String?)
    }

    @SuppressLint("ClickableViewAccessibility") // fixed by setting a click listener to avatarImageView
    override fun configureView(view: View): View {
        val noteBlockHolder = view.tag as UserActionNoteBlockHolder
        noteBlockHolder.mNameTextView.text = noteText.toString()
        var linkedText: String? = null
        if (hasUserUrlAndTitle()) {
            linkedText = userBlogTitle
        } else if (hasUserUrl()) {
            linkedText = userUrl
        }
        if (!TextUtils.isEmpty(linkedText)) {
            noteBlockHolder.mUrlTextView.text = linkedText
            noteBlockHolder.mUrlTextView.visibility = View.VISIBLE
        } else {
            noteBlockHolder.mUrlTextView.visibility = View.GONE
        }
        if (hasUserBlogTagline()) {
            noteBlockHolder.mTaglineTextView.text = userBlogTagline
            noteBlockHolder.mTaglineTextView.visibility = View.VISIBLE
        } else {
            noteBlockHolder.mTaglineTextView.visibility = View.GONE
        }
        var imageUrl = ""
        if (hasImageMediaItem()) {
            noteMediaItem?.url?.let {
                imageUrl = WPAvatarUtils.rewriteAvatarUrl(it, avatarSize)
            }
            if (!TextUtils.isEmpty(userUrl)) {
                noteBlockHolder.mAvatarImageView.setOnTouchListener(mOnGravatarTouchListener)
                noteBlockHolder.mRootView.isEnabled = true
                noteBlockHolder.mRootView.setOnClickListener(mOnClickListener)
            } else {
                noteBlockHolder.mAvatarImageView.setOnTouchListener(null)
                noteBlockHolder.mRootView.isEnabled = false
                noteBlockHolder.mRootView.setOnClickListener(null)
            }
        } else {
            noteBlockHolder.mRootView.isEnabled = false
            noteBlockHolder.mRootView.setOnClickListener(null)
            noteBlockHolder.mAvatarImageView.setOnTouchListener(null)
        }
        mImageManager.loadIntoCircle(
            noteBlockHolder.mAvatarImageView,
            ImageType.AVATAR_WITH_BACKGROUND,
            imageUrl
        )
        return view
    }

    override fun getViewHolder(view: View): Any = UserActionNoteBlockHolder(view)

    private inner class UserActionNoteBlockHolder(view: View) {
        val mRootView: View = view.findViewById(R.id.user_block_root_view)
        val mNameTextView: TextView = view.findViewById(R.id.user_name)
        val mUrlTextView: TextView = view.findViewById(R.id.user_blog_url)
        val mTaglineTextView: TextView = view.findViewById(R.id.user_blog_tagline)
        val mAvatarImageView: ImageView = view.findViewById(R.id.user_avatar)
    }

    private fun hasUserUrl(): Boolean = !TextUtils.isEmpty(userUrl)

    private fun hasUserUrlAndTitle(): Boolean = hasUserUrl() && !TextUtils.isEmpty(userBlogTitle)

    private fun hasUserBlogTagline(): Boolean = !TextUtils.isEmpty(userBlogTagline)

    @Suppress("MagicNumber")
    @JvmField
    val mOnGravatarTouchListener = OnTouchListener { v, event ->
        val animationDuration = 150
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0.5f)
                    .setDuration(animationDuration.toLong())
                    .setInterpolator(DecelerateInterpolator())
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(animationDuration.toLong())
                    .setInterpolator(DecelerateInterpolator())
                if (event.actionMasked == MotionEvent.ACTION_UP && mGravatarClickedListener != null) {
                    // Fire the listener, which will load the site preview for the user's site
                    // In the future we can use this to load a 'profile view' (currently in R&D)
                    v.performClick()
                }
            }
        }
        true
    }

    protected fun showBlogPreview() {
        val siteUrl = userUrl
        mGravatarClickedListener?.onGravatarClicked(
            noteData.getMetaIdsSiteIdOrZero(),
            noteData.getMetaIdsUserIdOrZero(), siteUrl
        )
    }
}
