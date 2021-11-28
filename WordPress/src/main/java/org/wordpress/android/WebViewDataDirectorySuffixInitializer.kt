package org.wordpress.android

import android.app.Application
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.TextUtils
import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Since Android P:
 * "Apps can no longer share a single WebView data directory across processes.
 * If your app has more than one process using WebView, CookieManager, or any other API in the android.webkit
 * package, your app will crash when the second process calls a WebView method."
 *
 * (see https://developer.android.com/about/versions/pie/android-9.0-migration)
 *
 * Also here: https://developer.android.com/about/versions/pie/android-9.0-changes-28#web-data-dirs
 *
 * "If your app must use instances of WebView in more than one process, you must assign a unique data
 * directory suffix for each process, using the WebView.setDataDirectorySuffix() method, before
 * using a given instance of WebView in that process."
 *
 * While we don't explicitly use a different process other than the default, making the directory suffix be
 * the actual process name will ensure there's one directory per process, should the Application's
 * onCreate() method be called from a different process any time.
 *
 */
@Singleton
class WebViewDataDirectorySuffixInitializer @Inject constructor() {
    init {
        if (VERSION.SDK_INT >= VERSION_CODES.P) {
            val procName = Application.getProcessName()
            if (!TextUtils.isEmpty(procName)) {
                WebView.setDataDirectorySuffix(procName)
            }
        }
    }
}
