package org.wordpress.android.ui.deeplinks

import org.wordpress.android.R

enum class DeepLinkEntryPoint {
    DEFAULT,
    URI_LINKS,
    WEB_LINKS;

    companion object {
        @JvmStatic
        fun fromResId(resId: Int?) = when {
            R.string.deep_linking_urilinks_alias_label == resId -> URI_LINKS
            R.string.deep_linking_weblinks_alias_label == resId -> WEB_LINKS
            else -> DEFAULT
        }
    }
}
