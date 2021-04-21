package org.wordpress.android.models

import org.wordpress.android.R
import javax.inject.Inject

class NoticonUtils
@Inject constructor() {
    fun noticonToGridicon(noticon: String): Int {
        // Transformation based on Calypso: https://git.io/JqUEC
        return when (noticon) {
            "\uf814" -> R.drawable.ic_mention_white_24dp
            "\uf300" -> R.drawable.ic_comment_white_24dp
            "\uf801" -> R.drawable.ic_add_white_24dp
            "\uf455" -> R.drawable.ic_info_white_24dp
            "\uf470" -> R.drawable.ic_lock_white_24dp
            "\uf806" -> R.drawable.ic_stats_alt_white_24dp
            "\uf805" -> R.drawable.ic_reblog_white_24dp
            "\uf408" -> R.drawable.ic_star_white_24dp
            "\uf804" -> R.drawable.ic_trophy_white_24dp
            "\uf467" -> R.drawable.ic_reply_white_24dp
            "\uf414" -> R.drawable.ic_notice_white_24dp
            "\uf418" -> R.drawable.ic_checkmark_white_24dp
            "\uf447" -> R.drawable.ic_cart_white_24dp
            else -> R.drawable.ic_info_white_24dp
        }
    }
}
