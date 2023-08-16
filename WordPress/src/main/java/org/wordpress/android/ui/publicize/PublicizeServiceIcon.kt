package org.wordpress.android.ui.publicize

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.models.PublicizeService

/**
 * Enum mapping the service id to the local icon resource id, so we can use custom icons for the Publicize Services.
 *
 * @param serviceId The id of the service, as returned by the Publicize API.
 * @param iconResId The local resource id of the icon to use for this service.
 *
 * @see PublicizeService
 */
enum class PublicizeServiceIcon(
    val serviceId: String,
    @DrawableRes val iconResId: Int,
) {
    FACEBOOK("facebook", R.drawable.ic_social_facebook),
    INSTAGRAM("instagram-business", R.drawable.ic_social_instagram),
    LINKEDIN("linkedin", R.drawable.ic_social_linkedin),
    MASTODON("mastodon", R.drawable.ic_social_mastodon),
    TUMBLR("tumblr", R.drawable.ic_social_tumblr),
    TWITTER("twitter", R.drawable.ic_social_twitter);

    companion object {
        /**
         * Returns the [PublicizeServiceIcon] for the given service name, or null if not found.
         *
         * @param serviceId The name of the service, as returned by the Publicize API.
         */
        @JvmStatic
        fun fromServiceId(serviceId: String): PublicizeServiceIcon? {
            return values().find { it.serviceId == serviceId }
        }
    }
}
