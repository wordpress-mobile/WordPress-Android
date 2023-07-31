package org.wordpress.android.ui

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

open class WPWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    WebView(context, attrs, defStyleAttr) {
    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }
}
