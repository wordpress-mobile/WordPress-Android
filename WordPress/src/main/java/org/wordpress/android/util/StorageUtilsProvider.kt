package org.wordpress.android.util

import android.content.Intent
import android.provider.Settings
import androidx.fragment.app.FragmentManager
import org.wordpress.android.R.string
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.widgets.StorageNotificationDialogFragment
import org.wordpress.android.widgets.StorageNotificationDialogFragment.DialogLabels
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageUtilsProvider @Inject constructor(
    private val contextProvider: ContextProvider
) {
    // Add more sources here for tracking purposes if using this class from other places than the editor
    enum class Source(val description: String) {
        EDITOR("editor")
    }

    fun notifyOnLowStorageSpace(fm: FragmentManager, source: Source) {
        if (
            isDeviceRunningOutOfSpace() &&
            AppPrefs.shouldShowStorageWarning() &&
            fm.findFragmentByTag(DIALOG_FRAGMENT_TAG) == null
        ) {
            val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            val context = contextProvider.getContext()
            val isInternalStorageSettingsResolved = intent.resolveActivity(context.packageManager) != null

            StorageNotificationDialogFragment.newInstance(
                dialogLabels = DialogLabels(
                    title = context.getString(string.storage_utils_dialog_title),
                    message = context.getString(string.storage_utils_dialog_message),
                    okLabel = if (isInternalStorageSettingsResolved) {
                        context.getString(string.storage_utils_dialog_ok_button)
                    } else {
                        context.getString(android.R.string.ok)
                    },
                    dontShowAgainLabel = context.getString(string.storage_utils_dialog_dont_show_button)
                ),
                isInternalStorageSettingsResolved = isInternalStorageSettingsResolved,
                source = source.description
            ).show(fm, DIALOG_FRAGMENT_TAG)
        }
    }

    private fun isDeviceRunningOutOfSpace(): Boolean {
        // if available space is at or below 10%, consider it low
        val appContext = contextProvider.getContext().applicationContext
        return (appContext.cacheDir.usableSpace * FULL_STORAGE_PERCENTAGE /
                appContext.cacheDir.totalSpace <= LOW_STORAGE_THRESHOLD)
    }

    companion object {
        private const val DIALOG_FRAGMENT_TAG = "storage-utils-dialog-fragment"
        private const val FULL_STORAGE_PERCENTAGE = 100
        private const val LOW_STORAGE_THRESHOLD = 10
    }
}
