package org.wordpress.android.ui.prefs.notifications

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.preference.DialogPreference
import org.wordpress.android.models.NotificationsSettings
class NotificationsSettingsDialogPreferenceX(
    context: Context,
    attrs: AttributeSet?,
    val channel: NotificationsSettings.Channel,
    val type: NotificationsSettings.Type,
    val blogId: Long,
    val settings: NotificationsSettings,
    val listener: NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener,
    val bloggingRemindersProvider: NotificationsSettingsDialogPreference.BloggingRemindersProvider? = null,
    @StringRes val dialogTitleRes: Int
) : DialogPreference(context, attrs)
