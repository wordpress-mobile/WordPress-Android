package org.wordpress.android.ui.sitecreation.services

import android.app.Notification
import android.content.Context
import org.wordpress.android.R
import org.wordpress.android.util.AutoForegroundNotification

object NewSiteCreationServiceNotification {
    private const val channelResId = R.string.notification_channel_normal_id
    private const val colorResId = R.color.blue_wordpress
    private const val drawableResId = R.drawable.ic_my_sites_white_24dp

    fun createCreatingSiteNotification(context: Context): Notification {
        return AutoForegroundNotification.progressIndeterminate(
                context,
                context.getString(channelResId),
                R.string.notification_new_site_creation_title,
                R.string.notification_new_site_creation_creating_site_subtitle,
                drawableResId,
                colorResId
        )
    }

    fun createSuccessNotification(context: Context): Notification {
        return AutoForegroundNotification.success(
                context,
                context.getString(channelResId),
                R.string.notification_site_creation_title_success,
                R.string.notification_site_creation_created,
                drawableResId,
                colorResId
        )
    }

    fun createFailureNotification(context: Context): Notification {
        return AutoForegroundNotification.failure(
                context,
                context.getString(channelResId),
                R.string.notification_new_site_creation_title,
                R.string.notification_new_site_creation_failed,
                drawableResId,
                colorResId
        )
    }
}
