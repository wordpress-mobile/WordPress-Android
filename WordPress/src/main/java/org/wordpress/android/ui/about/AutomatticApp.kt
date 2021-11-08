package org.wordpress.android.ui.about

import androidx.annotation.DrawableRes
import org.wordpress.android.R

enum class AutomatticApp(@DrawableRes val drawableRes: Int, val appUrl: String) {
    DAY_ONE(R.drawable.ic_dayone, "https://dayoneapp.com/"),
    JET_PACK(R.drawable.ic_jetpack, "https://jetpack.com/"),
    POCKET_CASTS(R.drawable.ic_pocketcasts, "https://www.pocketcasts.com/"),
    SIMPLE_NOTE(R.drawable.ic_simplenote, "https://simplenote.com/"),
    TUMBLR(R.drawable.ic_tumblr, "https://www.tumblr.com/"),
    WOO_COMMERCE(R.drawable.ic_woo, "https://woocommerce.com/"),
    WORD_PRESS(R.drawable.ic_wordpress, "https://wordpress.com/");
}
