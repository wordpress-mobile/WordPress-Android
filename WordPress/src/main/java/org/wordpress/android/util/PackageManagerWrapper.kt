package org.wordpress.android.util

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ComponentInfoFlags
import android.os.Build
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

    fun disableComponentEnabledSetting(name: String) {
        contextProvider.getContext().packageManager.setComponentEnabledSetting(
            ComponentName(contextProvider.getContext(), name),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
        )
    }

    fun enableComponentEnabledSetting(name: String) {
        contextProvider.getContext().packageManager.setComponentEnabledSetting(
            ComponentName(contextProvider.getContext(), name),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    fun disableReaderDeepLinks() = WPActivityUtils.disableReaderDeeplinks(contextProvider.getContext())
    fun enableReaderDeeplinks() = WPActivityUtils.enableReaderDeeplinks(contextProvider.getContext())


    @Suppress("SwallowedException")
    fun getActivityLabelResFromIntent(intent: Intent): Int? {
        intent.component?.let {
            try {
                val context = contextProvider.getContext()
                val activityInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getActivityInfo(
                        it,
                        ComponentInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getActivityInfo(it, PackageManager.GET_META_DATA)
                }
                return activityInfo.labelRes
            } catch (ex: PackageManager.NameNotFoundException) {
                AppLog.e(T.UTILS, "Unable to extract label res from activity info")
            }
        }
        return null
    }
}
