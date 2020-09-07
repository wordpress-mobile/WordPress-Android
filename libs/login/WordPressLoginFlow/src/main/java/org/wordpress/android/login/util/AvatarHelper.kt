package org.wordpress.android.login.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.wordpress.android.login.R
import org.wordpress.android.util.GravatarUtils
import org.wordpress.android.util.GravatarUtils.DefaultImage.STATUS_404

object AvatarHelper {
    @JvmStatic fun loadAvatarFromEmail(
        fragment: Fragment,
        email: String?,
        avatarView: ImageView,
        listener: AvatarRequestListener
    ) {
        val avatarSize = fragment.resources.getDimensionPixelSize(R.dimen.avatar_sz_login)
        val avatarUrl = GravatarUtils.gravatarFromEmail(email, avatarSize, STATUS_404)
        loadAvatarFromUrl(fragment, avatarUrl, avatarView, listener)
    }

    @JvmStatic fun loadAvatarFromUrl(
        fragment: Fragment,
        avatarUrl: String?,
        avatarView: ImageView,
        listener: AvatarRequestListener
    ) {
        Glide.with(fragment)
                .load(avatarUrl)
                .apply(RequestOptions.circleCropTransform())
                .apply(RequestOptions.placeholderOf(R.drawable.ic_user_circle_no_padding_grey_24dp))
                .apply(RequestOptions.errorOf(R.drawable.ic_user_circle_no_padding_grey_24dp))
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onRequestFinished()
                        return false
                    }

                    override fun onResourceReady(
                        drawable: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        listener.onRequestFinished()
                        return false
                    }
                })
                .into(avatarView)
    }

    interface AvatarRequestListener {
        fun onRequestFinished()
    }
}
