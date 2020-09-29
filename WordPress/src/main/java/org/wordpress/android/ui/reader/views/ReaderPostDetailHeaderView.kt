package org.wordpress.android.ui.reader.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.reader_post_detail_header_view.view.*
import org.wordpress.android.R
import org.wordpress.android.R.dimen
import org.wordpress.android.R.layout
import org.wordpress.android.WordPress
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.ReaderActivityLauncher
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * topmost view in post detail - shows blavatar, author name, blog name, and follow button
 */
class ReaderPostDetailHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var followListener: OnFollowListener? = null
    private var post: ReaderPost? = null

    @Inject lateinit var imageManager: ImageManager

    init {
        (context.applicationContext as WordPress).component().inject(this)
        View.inflate(context, layout.reader_post_detail_header_view, this)
    }

    fun setOnFollowListener(listener: OnFollowListener?) {
        followListener = listener
    }

    fun setPost(post: ReaderPost, isSignedInWPCom: Boolean) {
        this.post = post
        val hasBlogName = post.hasBlogName()
        val hasAuthorName = post.hasAuthorName()
        if (hasBlogName && hasAuthorName) {
            // don't show author name if it's the same as the blog name
            if (post.authorName == post.blogName) {
                text_header_title.text = post.authorName
                text_header_subtitle.visibility = View.GONE
            } else {
                text_header_title.text = post.authorName
                text_header_subtitle.text = post.blogName
            }
            image_header_blavatar.contentDescription = post.blogName
        } else if (hasBlogName) {
            text_header_title.text = post.blogName
            image_header_blavatar.contentDescription = post.blogName
            text_header_subtitle.visibility = View.GONE
        } else if (hasAuthorName) {
            text_header_title.text = post.authorName
            image_header_blavatar.contentDescription = post.authorName
            text_header_subtitle.visibility = View.GONE
        } else {
            text_header_title.setText(R.string.untitled)
            text_header_subtitle.visibility = View.GONE
        }
        text_header_title.setOnClickListener(clickListener)
        text_header_subtitle.setOnClickListener(clickListener)
        if (isSignedInWPCom) {
            header_follow_button.setIsFollowed(post.isFollowedByCurrentUser)
            header_follow_button.setOnClickListener { v -> toggleFollowStatus(v) }
        }
        header_follow_button.setVisible(isSignedInWPCom)
        showBlavatar(post.blogImageUrl, post.postAvatar)
    }

    private fun showBlavatar(blavatarUrl: String, avatarUrl: String) {
        val hasBlavatar = !TextUtils.isEmpty(blavatarUrl)
        AppLog.w(READER, avatarUrl)
        imageManager.cancelRequestAndClearImageView(image_header_blavatar)
        if (hasBlavatar) {
            val blavatarSz = resources.getDimensionPixelSize(dimen.reader_detail_header_blavatar)
            image_header_blavatar.layoutParams.height = blavatarSz
            image_header_blavatar.layoutParams.width = blavatarSz
            imageManager.load(
                    image_header_blavatar, BLAVATAR,
                    PhotonUtils.getPhotonImageUrl(blavatarUrl, blavatarSz, blavatarSz)
            )
        }
        image_header_blavatar.setVisible(hasBlavatar)
        image_header_blavatar.setOnClickListener(clickListener)
    }

    /*
     * click listener which shows blog preview
     */
    private val clickListener = OnClickListener { v ->
        post?.let {
            ReaderActivityLauncher.showReaderBlogPreview(v.context, post)
        }
    }

    private fun toggleFollowStatus(followButton: View) {
        if (!NetworkUtils.checkConnection(context)) {
            return
        }
        post?.let { post ->
            val isAskingToFollow = !post.isFollowedByCurrentUser
            if (isAskingToFollow) {
                followListener?.onFollowTapped(followButton, post.blogName, post.blogId)
            } else {
                followListener?.onFollowingTapped()
            }

            val listener = ActionListener { succeeded ->
                if (context == null) {
                    return@ActionListener
                }
                header_follow_button.isEnabled = true
                if (succeeded) {
                    post.isFollowedByCurrentUser = isAskingToFollow
                } else {
                    val errResId = if (isAskingToFollow) {
                        R.string.reader_toast_err_follow_blog
                    } else {
                        R.string.reader_toast_err_unfollow_blog
                    }
                    ToastUtils.showToast(context, errResId)
                    header_follow_button.setIsFollowed(!isAskingToFollow)
                }
            }

            // disable follow button until API call returns
            header_follow_button.isEnabled = false
            val result = if (post.isExternal) {
                ReaderBlogActions.followFeedById(post.feedId, isAskingToFollow, listener)
            } else {
                ReaderBlogActions.followBlogById(post.blogId, isAskingToFollow, listener)
            }
            if (result) {
                header_follow_button.setIsFollowedAnimated(isAskingToFollow)
            }
        }
    }
}
