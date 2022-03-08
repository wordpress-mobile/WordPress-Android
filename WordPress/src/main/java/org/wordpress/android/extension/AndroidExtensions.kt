package org.wordpress.android.extension

import androidx.fragment.app.FragmentActivity
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.ActivityLauncher

fun FragmentActivity.showSignInForResult() {
    if (BuildConfig.IS_JETPACK_APP) {
        ActivityLauncher.showSignInForResultJetpackOnly(this)
    } else {
        ActivityLauncher.showSignInForResultWpComOnly(this)
    }
}
