package org.wordpress.android.util

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerWrapper @Inject constructor(
    private val contextProvider: ContextProvider,
) {
    fun isPackageInstalled(packageName: String) =
            contextProvider.getContext().packageManager.getLaunchIntentForPackage(packageName) != null

    fun disableComponentEnabledSetting(cls: Class<*>) {
        contextProvider.getContext().packageManager.setComponentEnabledSetting(
                ComponentName(contextProvider.getContext(), cls),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    fun enableComponentEnableSetting(cls: Class<*>) {
        contextProvider.getContext().packageManager.setComponentEnabledSetting(
                ComponentName(contextProvider.getContext(), cls),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    fun disableComponentEnabledSetting(name: String) {
        contextProvider.getContext().packageManager.setComponentEnabledSetting(
                ComponentName(contextProvider.getContext(), name),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    fun enableComponentEnableSetting(name: String) {
        contextProvider.getContext().packageManager.setComponentEnabledSetting(
                ComponentName(contextProvider.getContext(), name),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    fun disableReaderDeepLinks() = WPActivityUtils.disableReaderDeeplinks(contextProvider.getContext())
    fun enableReaderDeeplinks() = WPActivityUtils.enableReaderDeeplinks(contextProvider.getContext())

    fun getActivityLabelResFromIntent(intent: Intent) : Int? {
        intent.component?.let {
            try {
                val context = contextProvider.getContext()
                val activityInfo = context.packageManager.getActivityInfo(it, PackageManager.GET_META_DATA)
                return activityInfo.labelRes
            } catch (ex: Exception) {
                AppLog.e(T.UTILS, "Unable to extract label res from activity info")
            }
        }
        return null
    }
}
