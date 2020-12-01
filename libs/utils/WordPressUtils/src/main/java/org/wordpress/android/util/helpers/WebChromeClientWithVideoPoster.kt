package org.wordpress.android.util.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.webkit.WebChromeClient
import androidx.annotation.DrawableRes

abstract class WebChromeClientWithVideoPoster(
    view: View?,
    @DrawableRes defaultVideoPosterRes: Int
) : WebChromeClient() {
    private val defaultPoster: Bitmap? = view?.context?.let {
        BitmapFactory.decodeResource(it.resources, defaultVideoPosterRes)
    }

    final override fun getDefaultVideoPoster(): Bitmap? {
        return super.getDefaultVideoPoster() ?: defaultPoster
    }
}
