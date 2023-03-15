package org.wordpress.android.ui.prefs.appicon

import android.app.Activity
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.showAsBottomSheet
import org.wordpress.android.ui.prefs.appicon.compose.AppIconSelector

object AppIconSelectorBottomSheet {
    @JvmStatic
    fun show(activity: Activity, appIconHelper: AppIconHelper, callback: AppIconSelectorCallback) {
        activity.showAsBottomSheet {
            AppTheme {
                AppIconSelector(
                    icons = appIconHelper.appIcons,
                    currentIcon = appIconHelper.getCurrentIcon(),
                    onIconSelected = { appIcon ->
                        callback.onAppIconSelected(appIcon)
                        hideBottomSheet()
                    },
                    onDismiss = this::hideBottomSheet
                )
            }
        }
    }
}
