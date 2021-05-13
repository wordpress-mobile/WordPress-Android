package org.wordpress.android.push.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.push.NotificationPushIds
import java.util.concurrent.TimeUnit

data class LocalPush(
    val type: Type,
    val delay: Long,
    val delayUnits: TimeUnit,
    @StringRes val title: Int,
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val uniqueId: Int? = null
) {
    val id = uniqueId ?: NotificationPushIds.LOCAL_NOTIFICATION_ID + type.ordinal

    enum class Type(val tag: String) {
        CREATE_SITE("create_site");

        companion object {
            fun fromTag(tag: String?): Type? {
                return when (tag) {
                    CREATE_SITE.tag -> CREATE_SITE
                    else -> null
                }
            }
        }
    }
}
