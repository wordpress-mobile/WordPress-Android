package org.wordpress.android.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.webkit.WebView
import androidx.appcompat.view.ContextThemeWrapper
import org.wordpress.android.R

// https://stackoverflow.com/questions/58028821/webview-crash-on-android-5-5-1-api-21-22-resourcesnotfoundexception-string-r
open class WPWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        WebView(context.getModifiedContextForOlderAPIs(), attrs, defStyleAttr) {
    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }
}

private fun Context.getModifiedContextForOlderAPIs(): Context {
    return if (Build.VERSION.SDK_INT in 21..22) {
        ContextThemeWrapper(createConfigurationContext(Configuration()), R.style.WordPress)
    } else this
}
