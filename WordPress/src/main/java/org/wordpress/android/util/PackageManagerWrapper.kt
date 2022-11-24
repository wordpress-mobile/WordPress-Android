package org.wordpress.android.util

import android.content.ComponentName
import android.content.pm.PackageManager
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerWrapper @Inject constructor(
    private val contextProvider: ContextProvider,
) {
    fun isComponentEnabledSettingEnabled(cls: Class<*>): Boolean {
        return when (getComponentEnabledSetting(cls)) {
            null -> false
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            else -> isActivityEnabled(cls)
        }
    }

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

    fun disableReaderDeepLinks() = WPActivityUtils.disableReaderDeeplinks(contextProvider.getContext())
    fun enableReaderDeeplinks() = WPActivityUtils.enableReaderDeeplinks(contextProvider.getContext())

    private fun getComponentEnabledSetting(cls: Class<*>) =
            try {
                contextProvider.getContext().packageManager.getComponentEnabledSetting(
                        ComponentName(contextProvider.getContext(), cls)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                AppLog.e(T.UTILS, e)
                null
            }

    private fun isActivityEnabled(cls: Class<*>) : Boolean {
        val context = contextProvider.getContext()
        val packageInfo =
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        for (activity in packageInfo.activities) {
            if (activity.name == cls.name) {
                return activity.enabled // This is the default value (set in AndroidManifest.xml)
            }
        }
        return false
    }
}
