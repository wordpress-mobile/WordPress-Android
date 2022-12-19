package org.wordpress.android.workers.notification.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.push.NotificationPushIds
import java.util.concurrent.TimeUnit

data class LocalNotification(
    val type: Type,
    val delay: Long,
    val delayUnits: TimeUnit,
    @StringRes val title: Int,
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val firstActionIcon: Int,
    @StringRes val firstActionTitle: Int,
    @DrawableRes val secondActionIcon: Int? = null,
    @StringRes val secondActionTitle: Int? = null,
    val uniqueId: Int? = null
) {
    val id = uniqueId ?: NotificationPushIds.LOCAL_NOTIFICATION_ID + type.ordinal

    enum class Type(val tag: String) {
        CREATE_SITE("create_site"),
        BLOGGING_PROMPTS_ONBOARDING("blogging_prompts_onboarding");

        companion object {
            fun fromTag(tag: String?): Type? {
                return when (tag) {
                    CREATE_SITE.tag -> CREATE_SITE
                    BLOGGING_PROMPTS_ONBOARDING.tag -> BLOGGING_PROMPTS_ONBOARDING
                    else -> null
                }
            }
        }
    }
}
